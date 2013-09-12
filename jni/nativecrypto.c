/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "nativecrypto.h"

#include <jni.h>

#include "crypto_scrypt.h"
#include <errno.h>
#include <stdlib.h>
#include <inttypes.h>

/**
 * Helper function to invoke scrypt's internal PBKDF2 function
 * with JNI arguments.
 */
jbyteArray pbkdf2SHA256_scrypt
  (JNIEnv *env, jclass jc, jbyteArray password, jbyteArray salt, jint c, jint dkLen) {

	jbyte *pj = (*env)->GetByteArrayElements(env, password, 0);
  uint8_t *p = (uint8_t *)pj;
  size_t passwordLength = (*env)->GetArrayLength(env, password);

  jbyte *sj = (*env)->GetByteArrayElements(env, salt, 0);
  uint8_t *s = (uint8_t *)sj;
  size_t saltLength = (*env)->GetArrayLength(env, salt);

  uint8_t *out = (uint8_t *) malloc(sizeof(uint8_t) * dkLen);
  if (out == NULL) {
    return out;
  }
  uint64_t cc = (uint64_t) c;
  size_t dk = (size_t) dkLen;

  PBKDF2_SHA256(p, passwordLength, s, saltLength, cc, out, dk);

  (*env)->ReleaseByteArrayElements(env, password, pj, JNI_ABORT);
  (*env)->ReleaseByteArrayElements(env, salt, sj, JNI_ABORT);

  jbyteArray result = (*env)->NewByteArray(env, dkLen);
  (*env)->SetByteArrayRegion(env, result, 0, dkLen, out);
  free(out);

  return result;
}

JNIEXPORT jbyteArray JNICALL Java_org_mozilla_gecko_sync_crypto_NativeCrypto_pbkdf2SHA256
  (JNIEnv *env, jclass jc, jbyteArray password, jbyteArray salt, jint c, jint dkLen) {
    return pbkdf2SHA256_scrypt(env, jc, password, salt, c, dkLen);
}
