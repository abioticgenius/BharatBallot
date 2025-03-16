from flask import Flask, request, jsonify
from flask_cors import CORS
from utils.encryption import encrypt_data, decrypt_data, sign_data, verify_signature
from utils.otp_service import send_otp, verify_otp
from utils.firestore_service import (
    create_user_profile, authenticate_user, store_vote,
    get_vote_count, get_user_profile
)
from utils.firebase_config import get_firestore_client
import uuid
import hashlib

# --------------------------------------------
# ✅ Initialize Flask App
# --------------------------------------------
app = Flask(__name__)
CORS(app)

# --------------------------------------------
# ✅ Get Firestore Client
# --------------------------------------------
db = get_firestore_client()

# --------------------------------------------
# ✅ User Registration
# --------------------------------------------
@app.route('/register', methods=['POST'])
def register():
    data = request.json
    aadhaar = data.get('aadhaar')
    phone = data.get('phone')
    email = data.get('email')

    profile_data = {
        "name": data.get('name'),
        "age": data.get('age'),
        "gender": data.get('gender'),
        "aadhaar": encrypt_data(aadhaar),
        "phone": phone,
        "email": email,
        "password": encrypt_data(data.get('password'))
    }

    # Send OTP to phone and email
    phone_otp = send_otp(phone)
    email_otp = send_otp(email)

    # Store user profile
    result = create_user_profile(aadhaar, profile_data)

    if result['status'] == 'success':
        return jsonify({
            "message": "OTP sent to phone and email",
            "phone_otp": phone_otp,
            "email_otp": email_otp
        }), 200
    else:
        return jsonify({"message": result['message']}), 400

# --------------------------------------------
# ✅ OTP Verification
# --------------------------------------------
@app.route('/verify-otp', methods=['POST'])
def verify_otp_route():
    data = request.json
    phone_verified = verify_otp(data.get('phone'), data.get('phone_otp'))
    email_verified = verify_otp(data.get('email'), data.get('email_otp'))

    if phone_verified and email_verified:
        return jsonify({"message": "OTP verification successful"}), 200
    else:
        return jsonify({"message": "OTP verification failed"}), 400

# --------------------------------------------
# ✅ User Login
# --------------------------------------------
@app.route('/login', methods=['POST'])
def login():
    data = request.json
    aadhaar = data.get('aadhaar')
    password = data.get('password')

    user = authenticate_user(aadhaar, password)
    if user['status'] == 'success':
        return jsonify({"message": "Login successful", "user_id": aadhaar}), 200
    else:
        return jsonify({"message": user['message']}), 401

# --------------------------------------------
# ✅ Admin Login
# --------------------------------------------
@app.route('/admin/login', methods=['POST'])
def admin_login():
    data = request.json
    username = data.get('username')
    password = data.get('password')

    # Simple admin check (improve security in production)
    if username == "admin" and password == "admin123":
        return jsonify({"message": "Admin login successful"}), 200
    else:
        return jsonify({"message": "Invalid admin credentials"}), 401

# --------------------------------------------
# ✅ Submit Vote
# --------------------------------------------
@app.route('/submit-vote', methods=['POST'])
def submit_vote():
    data = request.json
    voter_id = data.get('voter_id')
    candidate_id = data.get('candidate_id')
    vote_signature = data.get('vote_signature')

    # Encrypt vote and create Zero-Knowledge Proof (ZKP)
    encrypted_vote = encrypt_data(candidate_id)
    vote_id = str(uuid.uuid4())
    zkp_proof = hashlib.sha256(voter_id.encode()).hexdigest()

    vote_data = {
        "voter_id_hash": zkp_proof,
        "encrypted_vote": encrypted_vote,
        "signature": vote_signature
    }

    result = store_vote(vote_id, vote_data)
    if result['status'] == 'success':
        return jsonify({"message": "Vote submitted successfully"}), 200
    else:
        return jsonify({"message": result['message']}), 400

# --------------------------------------------
# ✅ Verify Vote Signature
# --------------------------------------------
@app.route('/verify-vote', methods=['POST'])
def verify_vote():
    data = request.json
    is_valid = verify_signature(data.get('vote'), data.get('signature'), data.get('public_key'))

    return jsonify({"valid": is_valid}), 200 if is_valid else 400

# --------------------------------------------
# ✅ Get Vote Count
# --------------------------------------------
@app.route('/vote-count', methods=['GET'])
def vote_count():
    election_id = request.args.get('election_id')
    result = get_vote_count(election_id)
    return jsonify(result), 200

# --------------------------------------------
# ✅ Decrypt Vote (For Admin)
# --------------------------------------------
@app.route('/decrypt-vote', methods=['POST'])
def decrypt_vote():
    data = request.json
    decrypted_vote = decrypt_data(data.get('encrypted_vote'))
    return jsonify({"decrypted_vote": decrypted_vote}), 200

# --------------------------------------------
# ✅ Run Flask App
# --------------------------------------------
if __name__ == '__main__':
    app.run(debug=True)
