import random
import string
import time
from twilio.rest import Client
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

# --------------------------------------------
# ✅ Configuration for OTP Services
# --------------------------------------------
OTP_EXPIRY_TIME = 300  # OTP validity in seconds (5 minutes)

# Twilio SMS Configuration
TWILIO_ACCOUNT_SID = 'your_twilio_account_sid'
TWILIO_AUTH_TOKEN = 'your_twilio_auth_token'
TWILIO_PHONE_NUMBER = '+1234567890'

# Email Configuration
SMTP_SERVER = 'smtp.gmail.com'
SMTP_PORT = 587
EMAIL_ADDRESS = 'your_email@gmail.com'
EMAIL_PASSWORD = 'your_email_password'

# --------------------------------------------
# ✅ In-memory OTP Storage
# --------------------------------------------
otp_storage = {}  # Format: { 'identifier': {'otp': '123456', 'timestamp': 1234567890} }

# --------------------------------------------
# ✅ OTP Generation
# --------------------------------------------
def generate_otp(length=6):
    """Generates a numeric OTP."""
    return ''.join(random.choices(string.digits, k=length))

# --------------------------------------------
# ✅ Send OTP via SMS using Twilio
# --------------------------------------------
def send_otp_sms(phone_number, otp):
    """Sends OTP via SMS."""
    try:
        client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)
        message = client.messages.create(
            body=f"Your Bharat Ballot OTP is: {otp}",
            from_=TWILIO_PHONE_NUMBER,
            to=phone_number
        )
        return {"status": "success", "message": "OTP sent via SMS."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

# --------------------------------------------
# ✅ Send OTP via Email
# --------------------------------------------
def send_otp_email(email, otp):
    """Sends OTP via Email."""
    try:
        msg = MIMEMultipart()
        msg['From'] = EMAIL_ADDRESS
        msg['To'] = email
        msg['Subject'] = "Bharat Ballot OTP Verification"

        body = f"Dear User,\n\nYour OTP for Bharat Ballot verification is: {otp}\nIt is valid for 5 minutes.\n\nThank you."
        msg.attach(MIMEText(body, 'plain'))

        server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT)
        server.starttls()
        server.login(EMAIL_ADDRESS, EMAIL_PASSWORD)
        text = msg.as_string()
        server.sendmail(EMAIL_ADDRESS, email, text)
        server.quit()

        return {"status": "success", "message": "OTP sent via Email."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

# --------------------------------------------
# ✅ Store OTP with Expiry
# --------------------------------------------
def store_otp(identifier, otp):
    """Stores the OTP with the current timestamp."""
    otp_storage[identifier] = {
        'otp': otp,
        'timestamp': time.time()
    }

# --------------------------------------------
# ✅ Verify OTP
# --------------------------------------------
def verify_otp(identifier, input_otp):
    """Verifies the OTP and checks for expiry."""
    if identifier not in otp_storage:
        return {"status": "error", "message": "No OTP request found for this identifier."}

    stored_otp_info = otp_storage[identifier]
    if time.time() - stored_otp_info['timestamp'] > OTP_EXPIRY_TIME:
        del otp_storage[identifier]
        return {"status": "error", "message": "OTP has expired."}

    if stored_otp_info['otp'] != input_otp:
        return {"status": "error", "message": "Invalid OTP."}

    del otp_storage[identifier]
    return {"status": "success", "message": "OTP verified successfully."}

# --------------------------------------------
# ✅ Public Functions for OTP Process
# --------------------------------------------
def send_otp(identifier, contact_type, contact_value):
    """Generates, stores, and sends OTP via specified contact method."""
    otp = generate_otp()
    store_otp(identifier, otp)

    if contact_type == "sms":
        return send_otp_sms(contact_value, otp)
    elif contact_type == "email":
        return send_otp_email(contact_value, otp)
    else:
        return {"status": "error", "message": "Invalid contact type."}
