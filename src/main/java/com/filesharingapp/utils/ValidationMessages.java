package com.filesharingapp.utils;

/**
 * ValidationMessages
 * ------------------
 * Baby English:
 *   - These are friendly text messages when user input is wrong.
 *   - We keep them simple and clear for the user.
 */
public final class ValidationMessages {

    private ValidationMessages() {
        // Utility class – no objects.
    }

    // --------- Common ----------
    public static final String NAME_REQUIRED    = "Please tell me your name.";
    public static final String MODE_INVALID     = "Pick HTTP, ZeroTier, or S3.";
    public static final String HOST_REQUIRED    = "Please enter your partner's IP or host.";
    public static final String PORT_INVALID     = "Port must be between 1 and 65535.";

    // --------- HTTP Path ----------
    public static final String HTTP_PATH_REQUIRED =
            "Please enter a valid HTTP path (example: /upload).";
    public static final String HTTP_PATH_INVALID =
            "HTTP path must start with '/' and contain only letters, numbers, and dashes.";

    // --------- Sender File Rules ----------
    public static final String FILE_REQUIRED    = "Please pick a file before starting.";
    public static final String FILE_NOT_FOUND   = "We could not find the file you selected.";
    public static final String FILE_TOO_LARGE   = "File is larger than the allowed limit.";
    public static final String FILE_EMPTY       = "The file is empty.";

    // --------- Receiver Rules ----------
    public static final String FOLDER_REQUIRED  = "Please choose a save folder.";
    public static final String FOLDER_DENIED    = "You do not have permission to save here.";

    // --------- ZeroTier ----------
    public static final String ZT_ID_INVALID =
            "ZeroTier ID must be a 16-digit hexadecimal string.";
    public static final String ZT_HELP_NETWORK =
            "Enter your ZeroTier network ID (16 hex digits). Example: 8056c2e21c000001.";
    public static final String ZT_HELP_PEER =
            "Make sure your peer is ONLINE in ZeroTier. Use 'zerotier-cli listpeers' to check.";

    // --------- S3 ----------
    public static final String S3_BUCKET_REQUIRED =
            "Please enter your S3 bucket name.";
    public static final String S3_REGION_REQUIRED =
            "Please enter AWS region (example: us-east-1).";
    public static final String S3_AUTH_REQUIRED =
            "AWS Access Key and Secret Key cannot be empty.";
    public static final String S3_TEST_BUCKET =
            "Testing S3 bucket connectivity…";
    public static final String S3_TEST_FAILED =
            "Could not connect to S3 bucket. Check your credentials and region.";

    // --------- AES Encryption ----------
    public static final String AES_KEY_REQUIRED =
            "Please enter an AES key for encryption.";
    public static final String AES_KEY_INVALID =
            "AES key must be 16, 24, or 32 characters long.";

    // --------- Metadata Mismatch ----------
    public static final String CHECKSUM_MISMATCH =
            "File checksum does not match. Transfer may be corrupted.";
    public static final String FILENAME_MISMATCH =
            "File name does not match expected metadata.";

    // --------- Resume ----------
    public static final String RESUME_QUESTION =
            "We found a partial file. Do you want to resume?";
}