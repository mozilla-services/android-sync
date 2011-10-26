package com.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.mozilla.android.sync.HKDF;

/*
 * This class tests the HKDF.java class.
 * The tests are the 3 HMAC-based test cases
 * from the RFC 5869 specification.
 */
public class HKDFTests {
    
    @Test
    public void testCase1() {
        
        String IKM =    "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b";
        String salt =   "000102030405060708090a0b0c";
        String info =   "f0f1f2f3f4f5f6f7f8f9";
        int L =         42;
        String PRK =    "077709362c2e32df0ddc3f0dc47bba63" +
                        "90b6c73bb50f9c3122ec844ad7c2b3e5";
        String OKM =    "3cb25f25faacd57a90434f64d0362f2a" +
                        "2d2d0a90cf1a5a4c5db02d56ecc4c5bf" +
                        "34007208d5b887185865";
        
        assertEquals(doStep1(IKM, salt, PRK), true);
        assertEquals(doStep2(PRK, info, L, OKM), true);        
    }
    
    @Test
    public void testCase2() {
        
        String IKM =    "000102030405060708090a0b0c0d0e0f" +
                        "101112131415161718191a1b1c1d1e1f" +
                        "202122232425262728292a2b2c2d2e2f" + 
                        "303132333435363738393a3b3c3d3e3f" +
                        "404142434445464748494a4b4c4d4e4f";
        String salt =   "606162636465666768696a6b6c6d6e6f" +
                        "707172737475767778797a7b7c7d7e7f" +
                        "808182838485868788898a8b8c8d8e8f" +
                        "909192939495969798999a9b9c9d9e9f" +
                        "a0a1a2a3a4a5a6a7a8a9aaabacadaeaf";
        String info =   "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf" +
                        "c0c1c2c3c4c5c6c7c8c9cacbcccdcecf" +
                        "d0d1d2d3d4d5d6d7d8d9dadbdcdddedf" +
                        "e0e1e2e3e4e5e6e7e8e9eaebecedeeef" +
                        "f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff";
        int L =         82;
        String PRK =    "06a6b88c5853361a06104c9ceb35b45c" +
                        "ef760014904671014a193f40c15fc244"; 
        String OKM =    "b11e398dc80327a1c8e7f78c596a4934" +
                        "4f012eda2d4efad8a050cc4c19afa97c" +
                        "59045a99cac7827271cb41c65e590e09" +
                        "da3275600c2f09b8367793a9aca3db71" +
                        "cc30c58179ec3e87c14c01d5c1f3434f" +
                        "1d87";
        
        assertEquals(doStep1(IKM, salt, PRK), true);
        assertEquals(doStep2(PRK, info, L, OKM), true);        
    }
    
    @Test
    public void testCase3() {
        
        String IKM =    "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"; 
        String salt =   "";
        String info =   "";
        int L =         42;
        String PRK =    "19ef24a32c717b167f33a91d6f648bdf" +
                        "96596776afdb6377ac434c1c293ccb04";
        String OKM =    "8da4e775a563c18f715f802a063c5a31" +
                        "b8a11f5c5ee1879ec3454e5f3c738d2d" +
                        "9d201395faa4b61a96c8";
        
        assertEquals(doStep1(IKM, salt, PRK), true);
        assertEquals(doStep2(PRK, info, L, OKM), true);        
    }
    
    /*
     * Helper to do step 1 of RFC 5869
     */
    private boolean doStep1(String IKM, String salt, String PRK) {
        byte[] prkResult = HKDF.hkdfExtract(hexStringToByteArray(salt), hexStringToByteArray(IKM));
        byte[] prkExpect = hexStringToByteArray(PRK);
        return Arrays.equals(prkResult, prkExpect);
    }
    
    /*
     * Helper to do step 2 of RFC 5869
     */
    private boolean doStep2(String PRK, String info, int L, String OKM) {
        byte[] okmResult = HKDF.hkdfExpand(hexStringToByteArray(PRK), hexStringToByteArray(info), L);
        byte[] okmExpect = hexStringToByteArray(OKM);
        return Arrays.equals(okmResult, okmExpect);
    }
    
    /*
     * Input: Hex string to be converted
     * Ouput: byte[] reprsentation of hex string
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
}