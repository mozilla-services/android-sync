/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "mozopenssl.h"

#include <jni.h>

#include <openssl/evp.h>
#include <openssl/hmac.h>

JNIEXPORT jbyteArray JNICALL Java_org_mozilla_gecko_sync_crypto_MozOpenSSL_pbkdf2SHA256
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
