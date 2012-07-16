#!/usr/local/bin/node

try {
    var optimist = require('optimist');
    var fs = require('fs');
    var jwcrypto = require('./lib/jwcrypto');
    require('./lib/algs/rs'); // initializes algorithms.
} catch (x) {
    console.log('Could not require optimist or jwcrypto.');
    console.log();
    console.log('Installation:\n$ git clone https://github.com/mozilla/jwcrypto.git\n$ cp gen_mockmyid.js jwcrypto/\n$ ./jwcrypto/gen_mockmyid.js');
    process.exit(1);
}

var argv = optimist
    .usage('Generate mockmyid.com certificate for USERNAME@mockmyid.com and assertion for AUDIENCE.'
           + '\n\n'
           + 'Usage: $0 -u USERNAME [-a AUDIENCE] [-v]'
           + '\n\n'
           + 'See https://mockmyid.com/, http://dancallahan.info/journal/introducing-mockmyid/,\nand https://github.com/callahad/mockmyid.')
    .options('username', {
                 alias: "u",
                 demand: true,
                 describe: "generate certificate for USERNAME@mockymid.com",
             })
    .options('audience', {
                 alias: "a",
                 default: "http://localhost:8080",
                 describe: "generate assertion for AUDIENCE",
             })
    .options('verbose', {
                 alias: "v",
                 default: false,
                 describe: "be verbose",
             })
    .argv;

var AUDIENCE = argv.audience;
var EMAIL = argv.username;
if (EMAIL.indexOf("@mockmyid.com") < 0) {
    // At least accept test@mockmyid.com.
    EMAIL = EMAIL + "@mockmyid.com";
}
var VERBOSE = argv.verbose;

// Taken from mockmyid.com.
var domainKeypair = {
    secretKey: jwcrypto.loadSecretKeyFromObject(
        { algorithm: 'RS',
          n: '15498874758090276039465094105837231567265546373975960480941122651107772824121527483107402353899846252489837024870191707394743196399582959425513904762996756672089693541009892030848825079649783086005554442490232900875792851786203948088457942416978976455297428077460890650409549242124655536986141363719589882160081480785048965686285142002320767066674879737238012064156675899512503143225481933864507793118457805792064445502834162315532113963746801770187685650408560424682654937744713813773896962263709692724630650952159596951348264005004375017610441835956073275708740239518011400991972811669493356682993446554779893834303',
          e: '65537',
          d: '6539906961872354450087244036236367269804254381890095841127085551577495913426869112377010004955160417265879626558436936025363204803913318582680951558904318308893730033158178650549970379367915856087364428530828396795995781364659413467784853435450762392157026962694408807947047846891301466649598749901605789115278274397848888140105306063608217776127549926721544215720872305194645129403056801987422794114703255989202755511523434098625000826968430077091984351410839837395828971692109391386427709263149504336916566097901771762648090880994773325283207496645630792248007805177873532441314470502254528486411726581424522838833'
        })
};

function genCertificateAndAssertion(email, audience, cb) {
jwcrypto.generateKeypair(
    { algorithm: "RS", keysize: 256 },
    function(err, keypair) {
        if (err) {
            return cb(err);
        }

        // 2 days.
        var certificateExpiration = new Date().valueOf() + (2 * 24 * 60 * 60 * 1000);
        jwcrypto.cert.sign(
            keypair.publicKey,
            { email: email },
            { issuer: 'mockmyid.com', issueAt: new Date(), expiresAt: certificateExpiration },
            null,
            domainKeypair.secretKey,

            function(err, certificate) {
                if (err) {
                    return cb(err);
                }


                // 48 hours.
                var assertionExpiration = new Date().valueOf() + (48 * 60 * 60 * 1000);
                jwcrypto.assertion.sign(
                    {},
                    {issuer:"127.0.0.1", expiresAt: assertionExpiration, issuedAt: new Date(), audience:audience},
                    keypair.secretKey,
                    function(err, assertion) {
                        if (err) {
                            return cb(err);
                        }
                        return cb(null, {certificate: certificate, assertion: assertion});
                    });
            });
    });
};

genCertificateAndAssertion(EMAIL, AUDIENCE, function (err, pair) {
                               if (err) {
                                   console.log(err);
                                   process.exit(1);
                               }
/*
                var stream = fs.createWriteStream("certificate.txt", {flags: 'w'});
                stream.once('open', function(fd) {
                                stream.write(cert);
                            });
                if (VERBOSE) {
                    console.log("wrote certificate.txt");
                }
*/

                               var stream = fs.createWriteStream("assertion.txt", {flags: 'w'});
                               stream.once('open', function(fd) {
                                               stream.write(pair.certificate);
                                               stream.write("~");
                                               stream.write(pair.assertion);
                                           });
                               if (VERBOSE) {
                                   console.log("wrote assertion.txt");
                               }
                           });
