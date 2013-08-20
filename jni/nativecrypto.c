/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "nativecrypto.h"

#include <jni.h>

// For pbkdf2.
#include <openssl/evp.h>
#include <openssl/hmac.h>

// For scrypt.
#include "crypto_scrypt.h"
#include <errno.h>
#include <stdlib.h>
#include <inttypes.h>

JNIEXPORT jbyteArray JNICALL Java_org_mozilla_gecko_sync_crypto_NativeCrypto_pbkdf2SHA256
  (JNIEnv *env, jclass jc, jbyteArray password, jbyteArray salt, jint c, jint dkLen) {

	jbyte *pj = (*env)->GetByteArrayElements(env, password, 0);
  unsigned char *p = (unsigned char *)pj;
  int passwordLength = (*env)->GetArrayLength(env, password);

  jbyte *sj = (*env)->GetByteArrayElements(env, salt, 0);
  unsigned char *s = (unsigned char *)sj;
  int saltLength = (*env)->GetArrayLength(env, salt);

  const EVP_MD *digest = EVP_sha256();
  unsigned char *out = (unsigned char *) malloc(sizeof(unsigned char) * dkLen);
  if (out == NULL) {
    return out;
  }

  int rc = PKCS5_PBKDF2_HMAC(p, passwordLength, s, saltLength, c, digest, dkLen, out);

  (*env)->ReleaseByteArrayElements(env, password, pj, JNI_ABORT);
  (*env)->ReleaseByteArrayElements(env, salt, sj, JNI_ABORT);

  jbyteArray result = (*env)->NewByteArray(env, dkLen);
  (*env)->SetByteArrayRegion(env, result, 0, dkLen, out);
  free(out);

  return result;
}

jbyteArray JNICALL Java_org_mozilla_gecko_sync_crypto_NativeCrypto_scrypt(JNIEnv *env, jclass cls, jbyteArray passwd, jbyteArray salt,
    jint N, jint r, jint p, jint dkLen)
{
    jint Plen = (*env)->GetArrayLength(env, passwd);
    jint Slen = (*env)->GetArrayLength(env, salt);
    jbyte *P = (*env)->GetByteArrayElements(env, passwd, NULL);
    jbyte *S = (*env)->GetByteArrayElements(env, salt,   NULL);
    uint8_t *buf = malloc(sizeof(uint8_t) * dkLen);
    jbyteArray DK = NULL;

    if (P == NULL || S == NULL || buf == NULL) goto cleanup;

    if (crypto_scrypt((uint8_t *) P, Plen, (uint8_t *) S, Slen, N, r, p, buf, dkLen)) {
        jclass e = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        char *msg;
        switch (errno) {
            case EINVAL:
                msg = "N must be a power of 2 greater than 1";
                break;
            case EFBIG:
            case ENOMEM:
                msg = "Insufficient memory available";
                break;
            default:
                msg = "Memory allocation failed";
        }
        (*env)->ThrowNew(env, e, msg);
        goto cleanup;
    }

    DK = (*env)->NewByteArray(env, dkLen);
    if (DK == NULL) goto cleanup;

    (*env)->SetByteArrayRegion(env, DK, 0, dkLen, (jbyte *) buf);

  cleanup:

    if (P) (*env)->ReleaseByteArrayElements(env, passwd, P, JNI_ABORT);
    if (S) (*env)->ReleaseByteArrayElements(env, salt,   S, JNI_ABORT);
    if (buf) free(buf);

    return DK;
}
