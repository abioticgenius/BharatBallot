from flask import Blueprint, request, jsonify
from firebase_admin import firestore
from utils.encryption import encrypt_data, decrypt_data
from utils.firestore_service import store_vote, get_votes_by_aadhaar, get_candidates, log_vote_action
from utils.zkp import generate_zkp, verify_zkp
from datetime import datetime
import uuid

vote_bp = Blueprint('vote', __name__)
db = firestore.client()

# --------------------------------------------
# ✅ Cast a Vote
# --------------------------------------------
@vote_bp.route('/cast-vote', methods=['POST'])
def cast_vote():
    try:
        data = request.json
        aadhaar = data.get('aadhaar')
        candidate_id = data.get('candidate_id')

        # Check if the user has already voted
        existing_votes = get_votes_by_aadhaar(aadhaar)
        if existing_votes:
            return jsonify({"error": "User has already voted."}), 400

        # Generate Zero-Knowledge Proof (ZKP) for voter anonymity
        zkp_proof = generate_zkp(aadhaar)

        # Encrypt the vote details
        encrypted_vote = {
            "aadhaar_zkp": zkp_proof,
            "candidate_id": encrypt_data(candidate_id),
            "timestamp": encrypt_data(datetime.utcnow().isoformat())
        }

        # Store the encrypted vote
        store_vote(uuid.uuid4().hex, encrypted_vote)

        # Log the vote action
        log_vote_action(f"Vote cast by user with Aadhaar (ZKP): {zkp_proof}")

        return jsonify({"message": "Vote successfully recorded."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Verify Vote with ZKP (Admin use)
# --------------------------------------------
@vote_bp.route('/verify-vote', methods=['POST'])
def verify_vote():
    try:
        data = request.json
        aadhaar = data.get('aadhaar')
        zkp_proof = data.get('zkp_proof')

        # Verify the ZKP without revealing actual Aadhaar
        if verify_zkp(aadhaar, zkp_proof):
            return jsonify({"message": "ZKP verification successful."}), 200
        else:
            return jsonify({"error": "ZKP verification failed."}), 400

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Fetch All Candidates (For Display)
# --------------------------------------------
@vote_bp.route('/candidates', methods=['GET'])
def candidates():
    try:
        candidates_list = get_candidates()
        return jsonify({"candidates": candidates_list}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Fetch Vote Count (Admin use)
# --------------------------------------------
@vote_bp.route('/vote-count', methods=['GET'])
def vote_count():
    try:
        candidates = get_candidates()
        vote_counts = {}

        for candidate in candidates:
            candidate_id = candidate['id']
            votes = db.collection('votes').where('candidate_id', '==', encrypt_data(candidate_id)).get()
            vote_counts[candidate_id] = len(votes)

        return jsonify({"vote_counts": vote_counts}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Fetch All Votes (Admin use)
# --------------------------------------------
@vote_bp.route('/all-votes', methods=['GET'])
def all_votes():
    try:
        votes_ref = db.collection('votes').get()
        votes = []
        for vote in votes_ref:
            vote_data = vote.to_dict()
            votes.append({
                "aadhaar_zkp": vote_data['aadhaar_zkp'],
                "candidate_id": decrypt_data(vote_data['candidate_id']),
                "timestamp": decrypt_data(vote_data['timestamp'])
            })

        return jsonify({"votes": votes}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500
