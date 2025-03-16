from utils.firebase_config import get_firestore_client
from utils.encryption import encrypt_data, decrypt_data
from datetime import datetime

# --------------------------------------------
# ✅ Get Firestore Client
# --------------------------------------------
db = get_firestore_client()

# --------------------------------------------
# ✅ User Profile Management
# --------------------------------------------
def create_user_profile(aadhaar_number, profile_data):
    """
    Creates and saves an encrypted user profile to Firestore.
    """
    try:
        encrypted_profile = {key: encrypt_data(str(value)) for key, value in profile_data.items()}
        db.collection('users').document(aadhaar_number).set(encrypted_profile)
        return {"status": "success", "message": "User profile created successfully."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def get_user_profile(aadhaar_number):
    """
    Retrieves and decrypts a user profile from Firestore.
    """
    try:
        user_doc = db.collection('users').document(aadhaar_number).get()
        if user_doc.exists:
            decrypted_profile = {key: decrypt_data(value) for key, value in user_doc.to_dict().items()}
            return {"status": "success", "data": decrypted_profile}
        else:
            return {"status": "error", "message": "User profile not found."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def authenticate_user(aadhaar_number, password):
    """
    Authenticates a user by Aadhaar number and password.
    """
    try:
        user_doc = db.collection('users').document(aadhaar_number).get()
        if user_doc.exists:
            user_data = user_doc.to_dict()
            decrypted_password = decrypt_data(user_data.get('password'))
            if decrypted_password == password:
                return {"status": "success", "message": "Authentication successful."}
            else:
                return {"status": "error", "message": "Invalid password."}
        else:
            return {"status": "error", "message": "User not found."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

# --------------------------------------------
# ✅ Vote Management
# --------------------------------------------
def store_vote(voter_id, candidate_id, election_id):
    """
    Saves an encrypted vote to Firestore.
    """
    try:
        vote_data = {
            "voter_id": encrypt_data(voter_id),
            "candidate_id": encrypt_data(candidate_id),
            "election_id": encrypt_data(election_id),
            "timestamp": encrypt_data(str(datetime.utcnow()))
        }
        db.collection('votes').add(vote_data)
        return {"status": "success", "message": "Vote recorded successfully."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def get_vote_count(election_id):
    """
    Counts the number of votes for a specific election.
    """
    try:
        votes_query = db.collection('votes').where('election_id', '==', encrypt_data(election_id)).stream()
        count = sum(1 for _ in votes_query)
        return {"status": "success", "count": count}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def get_votes(election_id):
    """
    Retrieves and decrypts all votes for a specific election.
    """
    try:
        votes_query = db.collection('votes').where('election_id', '==', encrypt_data(election_id)).stream()
        votes = []
        for vote in votes_query:
            vote_data = vote.to_dict()
            votes.append({
                "voter_id": decrypt_data(vote_data["voter_id"]),
                "candidate_id": decrypt_data(vote_data["candidate_id"]),
                "timestamp": decrypt_data(vote_data["timestamp"])
            })
        return {"status": "success", "data": votes}
    except Exception as e:
        return {"status": "error", "message": str(e)}

# --------------------------------------------
# ✅ Candidate Management
# --------------------------------------------
def save_candidate(candidate_id, candidate_data):
    """
    Saves candidate details to Firestore.
    """
    try:
        db.collection('candidates').document(candidate_id).set(candidate_data)
        return {"status": "success", "message": "Candidate saved successfully."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def get_all_candidates():
    """
    Retrieves all candidates from Firestore.
    """
    try:
        candidates_query = db.collection('candidates').stream()
        candidates = [candidate.to_dict() for candidate in candidates_query]
        return {"status": "success", "data": candidates}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def get_candidate(candidate_id):
    """
    Retrieves a specific candidate by ID.
    """
    try:
        candidate_doc = db.collection('candidates').document(candidate_id).get()
        if candidate_doc.exists:
            return {"status": "success", "data": candidate_doc.to_dict()}
        else:
            return {"status": "error", "message": "Candidate not found."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

# --------------------------------------------
# ✅ Admin Audit Logs
# --------------------------------------------
def log_admin_activity(admin_id, action):
    """
    Logs admin activities for audit purposes.
    """
    try:
        log_data = {
            "admin_id": admin_id,
            "action": action,
            "timestamp": str(datetime.utcnow())
        }
        db.collection('admin_logs').add(log_data)
        return {"status": "success", "message": "Admin activity logged."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def get_admin_logs():
    """
    Retrieves all admin activity logs.
    """
    try:
        logs_query = db.collection('admin_logs').order_by("timestamp", direction=firestore.Query.DESCENDING).stream()
        logs = [log.to_dict() for log in logs_query]
        return {"status": "success", "data": logs}
    except Exception as e:
        return {"status": "error", "message": str(e)}
