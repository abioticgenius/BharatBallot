from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes
from Crypto.Protocol.KDF import PBKDF2
from Crypto.Hash import SHA256
from Crypto.Signature import DSS
from Crypto.PublicKey import ECC
import base64
import os

# AES Configuration
KEY_LENGTH = 32  # 256 bits for AES-256
BLOCK_SIZE = AES.block_size  # 16 bytes for AES
SALT = os.getenv('ENCRYPTION_SALT', 'default_salt_value').encode()  # Use environment variable for security
SECRET_KEY = os.getenv('ENCRYPTION_SECRET_KEY', 'default_secret_key').encode()

# --------------------------------------------
# ✅ Generate AES Encryption Key
# --------------------------------------------
def generate_key():
    """
    Generates a secure key using PBKDF2 (Password-Based Key Derivation Function 2).
    """
    return PBKDF2(SECRET_KEY, SALT, dkLen=KEY_LENGTH)

# --------------------------------------------
# ✅ Pad Data to AES Block Size
# --------------------------------------------
def pad(data):
    padding_length = BLOCK_SIZE - len(data) % BLOCK_SIZE
    return data + chr(padding_length) * padding_length

# --------------------------------------------
# ✅ Unpad Data after Decryption
# --------------------------------------------
def unpad(data):
    return data[:-ord(data[len(data) - 1:])]

# --------------------------------------------
# ✅ Encrypt Data with AES-256
# --------------------------------------------
def encrypt_data(plain_text):
    try:
        key = generate_key()
        iv = get_random_bytes(BLOCK_SIZE)
        cipher = AES.new(key, AES.MODE_CBC, iv)
        encrypted_bytes = cipher.encrypt(pad(plain_text).encode())
        encrypted_data = base64.b64encode(iv + encrypted_bytes).decode('utf-8')
        return encrypted_data
    except Exception as e:
        raise Exception(f"Encryption failed: {str(e)}")

# --------------------------------------------
# ✅ Decrypt Data with AES-256
# --------------------------------------------
def decrypt_data(encrypted_text):
    try:
        key = generate_key()
        encrypted_data = base64.b64decode(encrypted_text)
        iv = encrypted_data[:BLOCK_SIZE]
        cipher = AES.new(key, AES.MODE_CBC, iv)
        decrypted_bytes = cipher.decrypt(encrypted_data[BLOCK_SIZE:])
        return unpad(decrypted_bytes.decode('utf-8'))
    except Exception as e:
        raise Exception(f"Decryption failed: {str(e)}")

# --------------------------------------------
# ✅ Sign Data with ECDSA
# --------------------------------------------
def sign_data(data: str, private_key_pem: str) -> str:
    try:
        private_key = ECC.import_key(private_key_pem)
        hasher = SHA256.new(data.encode('utf-8'))
        signer = DSS.new(private_key, 'fips-186-3')
        signature = signer.sign(hasher)
        return base64.b64encode(signature).decode('utf-8')
    except Exception as e:
        raise Exception(f"Signing failed: {str(e)}")

# --------------------------------------------
# ✅ Verify ECDSA Signature
# --------------------------------------------
def verify_signature(data: str, signature: str, public_key_pem: str) -> bool:
    try:
        public_key = ECC.import_key(public_key_pem)
        hasher = SHA256.new(data.encode('utf-8'))
        verifier = DSS.new(public_key, 'fips-186-3')
        verifier.verify(hasher, base64.b64decode(signature))
        return True
    except ValueError:
        return False
    except Exception as e:
        raise Exception(f"Signature verification failed: {str(e)}")
