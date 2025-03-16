from flask import Blueprint, request, jsonify
from firebase_admin import firestore
from utils.encryption import decrypt_data, verify_signature
from utils.firestore_service import get_all_votes, get_candidate_details, log_admin_action

admin_bp = Blueprint('admin', __name__)
db = firestore.client()

# --------------------------------------------
# ✅ Admin: View All Votes (Encrypted)
# --------------------------------------------
@admin_bp.route('/admin/view-votes', methods=['GET'])
def view_votes():
    try:
        votes = get_all_votes()
        return jsonify({"votes": votes}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Admin: Verify Vote Signature
# --------------------------------------------
@admin_bp.route('/admin/verify-vote-signature', methods=['POST'])
def verify_vote_signature():
    data = request.json
    vote = data.get('vote')
    signature = data.get('signature')
    public_key = data.get('public_key')

    is_valid = verify_signature(vote, signature, public_key)
    return jsonify({"valid": is_valid}), 200 if is_valid else 400

# --------------------------------------------
# ✅ Admin: Decrypt and Count Votes
# --------------------------------------------
@admin_bp.route('/admin/decrypt-and-count', methods=['POST'])
def decrypt_and_count():
    try:
        votes = get_all_votes()
        candidate_count = {}

        for vote in votes:
            decrypted_vote = decrypt_data(vote['encrypted_vote'])
            candidate_count[decrypted_vote] = candidate_count.get(decrypted_vote, 0) + 1

        log_admin_action("Decrypted and counted votes.")

        return jsonify({"vote_count": candidate_count}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Admin: View Candidate Details
# --------------------------------------------
@admin_bp.route('/admin/candidates', methods=['GET'])
def get_candidates():
    try:
        candidates = get_candidate_details()
        return jsonify({"candidates": candidates}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Admin: Declare Results
# --------------------------------------------
@admin_bp.route('/admin/declare-results', methods=['POST'])
def declare_results():
    try:
        votes = get_all_votes()
        candidate_count = {}

        for vote in votes:
            decrypted_vote = decrypt_data(vote['encrypted_vote'])
            candidate_count[decrypted_vote] = candidate_count.get(decrypted_vote, 0) + 1

        winner = max(candidate_count, key=candidate_count.get)
        db.collection('election_results').document('result').set({
            "winner": winner,
            "vote_count": candidate_count
        })

        log_admin_action(f"Declared results with winner: {winner}")

        return jsonify({"winner": winner, "vote_count": candidate_count}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Admin: View Audit Logs
# --------------------------------------------
@admin_bp.route('/admin/audit-logs', methods=['GET'])
def view_audit_logs():
    try:
        logs_ref = db.collection('audit_logs').order_by('timestamp', direction=firestore.Query.DESCENDING).stream()
        logs = [{log.id: log.to_dict()} for log in logs_ref]
        return jsonify({"logs": logs}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500
