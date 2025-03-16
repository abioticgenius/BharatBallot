from flask import Blueprint, request, jsonify
from firebase_admin import firestore
from werkzeug.security import generate_password_hash, check_password_hash
from utils.encryption import encrypt_data, decrypt_data
from utils.otp_service import send_otp, verify_otp
from utils.firestore_service import store_user_profile, get_user_by_aadhaar, log_admin_action
import uuid

auth_bp = Blueprint('auth', __name__)
db = firestore.client()

# --------------------------------------------
# ✅ User Registration
# --------------------------------------------
@auth_bp.route('/register', methods=['POST'])
def register():
    try:
        data = request.json
        aadhaar = data.get('aadhaar')
        phone = data.get('phone')
        email = data.get('email')
        password = data.get('password')
        profile = {
            "name": data.get('name'),
            "age": data.get('age'),
            "gender": data.get('gender'),
            "aadhaar": aadhaar,
            "phone": phone,
            "email": email,
            "password": generate_password_hash(password)
        }

        # Check if user already exists
        existing_user = get_user_by_aadhaar(aadhaar)
        if existing_user:
            return jsonify({"error": "User already registered."}), 400

        # Send OTP for verification
        send_otp(phone, "phone")
        send_otp(email, "email")

        # Encrypt sensitive data
        encrypted_profile = {k: encrypt_data(str(v)) for k, v in profile.items()}

        # Store profile in Firestore
        store_user_profile(aadhaar, encrypted_profile)
        return jsonify({"message": "OTP sent to phone and email for verification."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ OTP Verification
# --------------------------------------------
@auth_bp.route('/verify-otp', methods=['POST'])
def verify_otp_route():
    try:
        data = request.json
        phone = data.get('phone')
        email = data.get('email')
        phone_otp = data.get('phone_otp')
        email_otp = data.get('email_otp')

        phone_verified = verify_otp(phone, phone_otp)
        email_verified = verify_otp(email, email_otp)

        if phone_verified and email_verified:
            return jsonify({"message": "OTP verification successful."}), 200
        else:
            return jsonify({"error": "OTP verification failed."}), 400

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ User Login
# --------------------------------------------
@auth_bp.route('/login', methods=['POST'])
def login():
    try:
        data = request.json
        aadhaar = data.get('aadhaar')
        password = data.get('password')

        user_doc = get_user_by_aadhaar(aadhaar)
        if not user_doc:
            return jsonify({"error": "User not found."}), 404

        user_data = {k: decrypt_data(v) for k, v in user_doc.items()}
        if not check_password_hash(user_data.get('password'), password):
            return jsonify({"error": "Invalid credentials."}), 401

        # Send OTP for final verification
        send_otp(user_data['phone'], "phone")
        send_otp(user_data['email'], "email")

        return jsonify({"message": "OTP sent for verification."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Biometric Authentication Placeholder
# --------------------------------------------
@auth_bp.route('/biometric-auth', methods=['POST'])
def biometric_auth():
    try:
        data = request.json
        biometric_token = data.get('biometric_token')

        # Placeholder logic for biometric validation (to be replaced with actual validation)
        if biometric_token == "valid_token":
            return jsonify({"message": "Biometric authentication successful."}), 200
        else:
            return jsonify({"error": "Biometric authentication failed."}), 401

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Admin Login
# --------------------------------------------
@auth_bp.route('/admin/login', methods=['POST'])
def admin_login():
    try:
        data = request.json
        username = data.get('username')
        password = data.get('password')

        admin_ref = db.collection('admins').document(username).get()
        if not admin_ref.exists:
            return jsonify({"error": "Admin not found."}), 404

        admin_data = admin_ref.to_dict()
        if not check_password_hash(admin_data['password'], password):
            return jsonify({"error": "Invalid credentials."}), 401

        send_otp(admin_data['phone'], "phone")
        send_otp(admin_data['email'], "email")
        log_admin_action(f"Admin {username} initiated login.")

        return jsonify({"message": "OTP sent for verification."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------------------------
# ✅ Admin OTP Verification
# --------------------------------------------
@auth_bp.route('/admin/verify-otp', methods=['POST'])
def admin_verify_otp():
    try:
        data = request.json
        username = data.get('username')
        phone_otp = data.get('phone_otp')
        email_otp = data.get('email_otp')

        admin_ref = db.collection('admins').document(username).get()
        admin_data = admin_ref.to_dict()

        phone_verified = verify_otp(admin_data['phone'], phone_otp)
        email_verified = verify_otp(admin_data['email'], email_otp)

        if phone_verified and email_verified:
            log_admin_action(f"Admin {username} successfully verified OTP.")
            return jsonify({"message": "Admin OTP verification successful."}), 200
        else:
            return jsonify({"error": "OTP verification failed."}), 400

    except Exception as e:
        return jsonify({"error": str(e)}), 500
