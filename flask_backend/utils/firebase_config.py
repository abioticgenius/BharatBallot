import firebase_admin
from firebase_admin import credentials, firestore

# --------------------------------------------
# ✅ Firebase Initialization (Singleton Pattern)
# --------------------------------------------
def get_firestore_client():
    if not firebase_admin._apps:
        cred = credentials.Certificate(r'C:\Users\prosh\OneDrive\Desktop\ros_files\Python_programs\BharatBallot3\flask_backend\serviceAccountKey.json')
        firebase_admin.initialize_app(cred)
    return firestore.client()
