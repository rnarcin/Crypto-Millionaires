package com.example.cryptomillionaires


import android.os.Bundle
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.cryptomillionaires.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.math.BigInteger
import java.net.URISyntaxException
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.*
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var socket: Socket
    private lateinit var binding: ActivityMainBinding
    private lateinit var newMessage: Emitter.Listener
    private lateinit var newTransfer: Emitter.Listener

    val receiver = ObliviousTransferReceiver()
    val sender = ObliviousTransferSender()
    private lateinit var cp: CommunicationProtocol
    val meter = 0.000008993

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViewById<Switch>(R.id.switch1).setOnCheckedChangeListener { _, _ ->
            findViewById<TextInputEditText>(R.id.latiude).isEnabled = !findViewById<TextInputEditText>(R.id.latiude).isEnabled
            findViewById<TextInputEditText>(R.id.longitude).isEnabled = !findViewById<TextInputEditText>(R.id.longitude).isEnabled
        }


        try {
            val socketUrl: String = "https://192.168.0.45:8765"
            val hostnameVerifier = HostnameVerifier { hostname, session -> true }
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<java.security.cert.X509Certificate?>?,
                    authType: String?
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<java.security.cert.X509Certificate?>?,
                    authType: String?
                ) {
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate?>? {
                    return arrayOfNulls(0)
                }
            })

            val client_key =
                "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCvbdDoVrj+kxXf\n" +
                        "B2KB8I1p74zl9pSFxQhQ3WQz/siqzIJAt3Zv9HOddQugVih23kvJNUiGzItlg98t\n" +
                        "dkGSm9t28yyjRjwmvmZfudMDQTUTx0Q04/V2GHfckkZ5+CweJH8ZzUmc3bgvqw6F\n" +
                        "cXJu6uOPyoTlNOu0Vo0rnHEseb22gEOJxXNzS2Iz2E0bO1KuuEZGeA1FWzluDMM1\n" +
                        "0wnRDcvwc3NS/4RiaCZPgAx7N+Hp5dCrfhbp+p/YrId6GO2qwefohUxwGc2mDlyl\n" +
                        "yzNF98kBlDX8KotbKFbT6UFEbJAb6LlLyeIbEONmlDDyOdguVaMN0Ofifo3gzObE\n" +
                        "SjM8Rd5dAgMBAAECggEBAJFsv5lbCtA+rgrM3GRLPW9sHakk18pso5Ald0ODGmY8\n" +
                        "Ul3DuHLmelE1DgZEZhbkAH8zpIPaXFQzFXdiaMlYWn4o6AAR4RtdCxCfwsUX8SAM\n" +
                        "OAaAUqMrdXFr8PB7QldaeeKxwyw+wMkDTG5itTItladFgxVe4WYFudFFRqxP32zB\n" +
                        "V3WjoDuNtzvMLHhA2h4zYwYHH+8aT3OHqQP1n6/SGwJlSdccBAKhcAB7BujvzTvA\n" +
                        "++SkMD+VodekcCFQ6WV5lsJ+UpWOFLIyGa7T6bEUuNd2hWEXv0SJ9ImcRXrd34OT\n" +
                        "E5aO9fz8o4wpVnz/KydGpU8+0ls+U8s5pMBnQZYdoCECgYEA3oeTWdV9VYrFK8R2\n" +
                        "H7sKuOqPdcbV9LTQ2fj1nWqE+N+s2/HjGbf/ckhg2dGmaJf9wA9uhSG9+rKao5rj\n" +
                        "/PwZvDWrQeE/5UMezNm0jQBanynGMX2DPorRi5eHcMOfWfcly+jyNuLpxwdCaEVP\n" +
                        "xpOa/9R9mJNCAN2JTITINlKjN3sCgYEAydClugVUk6Vm7eZB7VR+1wMfQw+3+khC\n" +
                        "rNSGtAlqIDXBSzX1RcRH+Em31fJno4+8504QwLJ1Qlubj3qgXRs1xzePCb+DzBzJ\n" +
                        "lSVUF9RN6ldzG33gG4+4f9M90XMaEHo+9EblogpWro6ud9xsbJMRbpviLHfB7Vvd\n" +
                        "LhX8CpJB7gcCgYBU/8JH+SUjrJr9ydA49I/27BmaKjYFf5+a8t6Wn15lVcLITI+r\n" +
                        "fj3DoGPmL495ujzBxOM5VRclEF3DCmH1ezI1Uua0hl6KquWz56Bwj2cODr3Wn5On\n" +
                        "Kw05XcrtRruyeJXmWndSgcA8Nsil3XvIZQ86kybaRb1baUONh9aV8WDIywKBgQCN\n" +
                        "8sOeY8FpWB1dl2cnlxbgBMIoWRX+ZHUBOzrDxxtuLGEYlYs+1yAH81HqmeVrld44\n" +
                        "kyAVaXAwjF6OPKIu2sSoqttg7+Oz3UusK+VKHXnwKjI8U/0dZalPvzTr7RmYHWPK\n" +
                        "TVOW9c6iWJk+lTtt4sOQGPSsck9ktAiOMchlZxoYYQKBgDQX/uYULD2vAijI1az6\n" +
                        "z81crd9akZo5fvdFZg5aoemwmpdOsayJLBhQjPIdNHWwh8H9kpBFYZQ4ag6919zV\n" +
                        "KNnhevI02HSDtW264v5/NTmBwf6UBh5IpSufGj14DFycyJX1Kwt+DXaPa8rZ164B\n" +
                        "c2f9to2AEzx7OagCb00Kw3P2\n"

            Log.d("HEX", client_key.toByteArray().joinToString("") { "%02x".format(it) })

            val client_certificate =
                "MIIDETCCAfkCFAjeatOXkloRirbrMdv1vajpSZw+MA0GCSqGSIb3DQEBCwUAMEUx\n" +
                        "CzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRl\n" +
                        "cm5ldCBXaWRnaXRzIFB0eSBMdGQwHhcNMjEwNjIzMDAxMjAyWhcNMjIwNjIzMDAx\n" +
                        "MjAyWjBFMQswCQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UE\n" +
                        "CgwYSW50ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
                        "AQ8AMIIBCgKCAQEAr23Q6Fa4/pMV3wdigfCNae+M5faUhcUIUN1kM/7IqsyCQLd2\n" +
                        "b/RznXULoFYodt5LyTVIhsyLZYPfLXZBkpvbdvMso0Y8Jr5mX7nTA0E1E8dENOP1\n" +
                        "dhh33JJGefgsHiR/Gc1JnN24L6sOhXFyburjj8qE5TTrtFaNK5xxLHm9toBDicVz\n" +
                        "c0tiM9hNGztSrrhGRngNRVs5bgzDNdMJ0Q3L8HNzUv+EYmgmT4AMezfh6eXQq34W\n" +
                        "6fqf2KyHehjtqsHn6IVMcBnNpg5cpcszRffJAZQ1/CqLWyhW0+lBRGyQG+i5S8ni\n" +
                        "GxDjZpQw8jnYLlWjDdDn4n6N4MzmxEozPEXeXQIDAQABMA0GCSqGSIb3DQEBCwUA\n" +
                        "A4IBAQBNXxcKtfKvjCLq2TfO+htJimL7ssFPZSkHfyCWG80wG2py+wHo6/fHJcW8\n" +
                        "Zpe7o+F8NhUvoxUa0VZmNxfZ/Qah0uuIjsvNYC6D1a4xzUtdEHrmpNfsqnPS/A5k\n" +
                        "Xq9koC0Fp9aAuAaS33ZofnwZprjnaD2je+yVnNeq0Y2BqQbZkHy2dPI309CRW3oZ\n" +
                        "GXym8bEKG4t1ri8dGistvGXZmrgrsPO/PbM6HgRfy9CVK32aKvevc+DAAyT5IYyH\n" +
                        "m3utSu+kv8DdujjOW3hdYh2ZxO9CfDjEqu62k9dRHqTRPHRHWJ0e7iPhYnv8JS0L\n" +
                        "Hm1uR1KbK0///Ax0IItY0v01ct/P\n"

            val sslContext = SSLContext.getInstance("SSL")
            val key = KeyFactory.getInstance("RSA").generatePrivate(
                PKCS8EncodedKeySpec(
                    Base64.decode(
                        client_key,
                        Base64.DEFAULT
                    )
                )
            ) as RSAPrivateKey
            val certificate = CertificateFactory.getInstance("X.509").generateCertificate(
                Base64.decode(client_certificate, Base64.DEFAULT).inputStream()
            )
            val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
            keystore.load(null)
            keystore.setCertificateEntry("cert", certificate)
            keystore.setKeyEntry(
                "key",
                key,
                "password".toCharArray(),
                arrayOf<Certificate>(certificate)
            )
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(keystore, "password".toCharArray())
            sslContext.init(kmf.keyManagers, trustAllCerts, null)
            val sslSocketFactory = sslContext.socketFactory
            val trustManager = trustAllCerts[0] as X509TrustManager
            val okHttpClient = OkHttpClient.Builder()
                .hostnameVerifier(hostnameVerifier)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build()

            val opts = IO.Options()
            opts.callFactory = okHttpClient
            opts.webSocketFactory = okHttpClient
            socket = IO.socket(socketUrl, opts)

            cp = CommunicationProtocol(this)

            newMessage = object : Emitter.Listener {
                override fun call(vararg args: Any?) {
                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                        val textView = findViewById<TextView>(R.id.sample_text)
                        textView.text = args[0].toString()
                    })
                }

            }

            newTransfer = object : Emitter.Listener {
                override fun call(vararg args: Any?) {
                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                        val jObj = JSONObject(args[0].toString())
                        when (jObj.getString("state").toInt()) {
                            -1 -> {
                                Toast.makeText(this@MainActivity, "They asked.", Toast.LENGTH_SHORT).show()
                                cp.start()
                                socket.emit("transfer", JSONObject("""{"state": 0}"""))
                            }
                            0 -> {
                                socket.emit("transfer", JSONObject("""{"state": 1, "i":0}"""))
                            }
                            1 -> {
                                socket.emit(
                                    "transfer",
                                    JSONObject("""{"state": 2, "modulus":"${sender.modulus}", "public_exponent":"${sender.public_exponent}"}""")
                                )
                                sender.generate_xs()
                                socket.emit(
                                    "transfer",
                                    JSONObject("""{"state": 3, "i":${jObj.getString("i")}, "x0":"${sender.x0}", "x1":"${sender.x1}"}""")
                                )
                            }
                            2 -> receiver.set_modulus_exponent(
                                jObj.getString("modulus").toBigInteger(),
                                jObj.getString("public_exponent").toBigInteger()
                            )
                            3 -> {
                                receiver.set_messages(
                                    jObj.getString("x0").toBigInteger(),
                                    jObj.getString("x1").toBigInteger()
                                )
                                receiver.generate_v(
                                    cp.latiude_binary[jObj.getString("i").toInt()].toString()
                                        .toInt()
                                )
                                socket.emit(
                                    "transfer",
                                    JSONObject("""{"state": 5, "i":${jObj.getString("i")}, "v": "${receiver.v}"}""")
                                )
                            }
                            5 -> {
                                sender.set_v(jObj.getString("v").toBigInteger())
                                sender.generate_ks()
                                sender.generate_ms(
                                    cp.Kp[0][jObj.getString("i").toInt()],
                                    cp.Kp[1][jObj.getString("i").toInt()]
                                )
                                socket.emit(
                                    "transfer",
                                    JSONObject("""{"state": 7, "i":${jObj.getString("i")}, "mp0":"${sender.mp0}", "mp1":"${sender.mp1}"}""")
                                )
                            }
                            7 -> {
                                receiver.set_ms(
                                    jObj.getString("mp0").toBigInteger(),
                                    jObj.getString("mp1").toBigInteger()
                                )
                                val i = jObj.getString("i").toInt()
                                cp.numbers.add(
                                    receiver.unblind(
                                        cp.latiude_binary[i].toString().toInt()
                                    )
                                )
                                if (i < cp.d - 1)
                                    socket.emit(
                                        "transfer",
                                        JSONObject("""{"state": 1, "i":${i + 1}}""")
                                    )
                                else {
                                    socket.emit("transfer", JSONObject("""{"state": 8}"""))
                                }
                            }
                            8 -> {
                                var N = BigInteger.ZERO
                                for (v in cp.S) {
                                    N = N.xor(v.reversed().toBigInteger(2))
                                }
                                N = cp.bitwise_rotation(N.toString(2), cp.u)
                                socket.emit("transfer", JSONObject("""{"state": 9, "N":"${N}"}"""))
                            }
                            9 -> {
                                var answer = jObj.getString("N").toBigInteger()

                                for (number in cp.numbers)
                                    answer = answer.xor(number)
                                val result = cp.analize_answer(answer)
                                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                            }
                            else -> print("Unknown state")
                        }
                    })
                }

            }

            socket.on("message", newMessage)
            socket.on("transfer", newTransfer)
            socket.on(Socket.EVENT_CONNECT_ERROR, newMessage)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    /** Called when the user taps the Send button  */
    fun askQuestion(view: View) {
        cp.start()
        socket.emit("transfer", JSONObject("""{"state": -1}"""))
    }

    fun connectToServer(view: View) {
        socket.connect()
        findViewById<Switch>(R.id.switch1).isEnabled = true
        findViewById<TextInputEditText>(R.id.latiude).isEnabled = true
        findViewById<TextInputEditText>(R.id.longitude).isEnabled = true
        findViewById<TextInputEditText>(R.id.question).isEnabled = true
        findViewById<Button>(R.id.button2).isEnabled = true
    }

    class CommunicationProtocol(activity: MainActivity) {
        var activity = activity
        var latiude = 0.0
        var latiude_binary = ""
        var longitude = 0.0
        var longitude_binary = ""
        var d = 0
        var offset = 0
        lateinit var key_pair: KeyPair
        lateinit var public_key: RSAPublicKey
        lateinit var modulus: BigInteger
        lateinit var public_exponent: BigInteger
        lateinit var K: Array<Array<String>>
        val k = ObliviousTransfer().length_of_key
        var u = 0
        var v = 0
        lateinit var S: Array<String>
        lateinit var Kp: Array<Array<BigInteger>>
        lateinit var numbers: MutableList<BigInteger>


        fun start() {
            latiude = activity.findViewById<TextView>(R.id.latiude).text.toString().toDouble()
            longitude = activity.findViewById<TextView>(R.id.latiude).text.toString().toDouble()
            latiude_binary = latiude.toInt().toString(2).reversed()
            longitude_binary = longitude.toInt().toString(2).reversed()
            d = latiude_binary.length
            key_pair = ObliviousTransfer().generate_rsa()
            public_key = key_pair.public as RSAPublicKey
            modulus = public_key.modulus
            public_exponent = public_key.publicExponent
            K = Array(2) { Array(d) { ("0".repeat(k)) } }
            S = Array(d) { ("0".repeat(k)) }
            Kp = Array(2) { Array(latiude_binary.length) { ("1".repeat(k)).toBigInteger(2) } }
            u = Random.nextInt(1, 2 * k)
            v = Random.nextInt(0, k + 1)
            numbers = mutableListOf()
            for (i in 0 until d) {
                for (j in v until k) {
                    K[0][i] = set_bit(K[0][i], j)
                    K[1][i] = set_bit(K[1][i], j)
                }
                val l = if (latiude_binary[i] == '1') 0 else 1
                for (j in 0 until 2 * i) {
                    K[l][i] = set_bit(K[l][i], j)
                }
                val m = 2 * i
                K[l][i] = set_bit(K[l][i], m+1, 1)
                K[l][i] = set_bit(K[l][i], m, latiude_binary[i].toString().toInt())
                S[i] = generate_big_Int(k)
            }

            var xor_s = 0
            var xor_k = 0
            S[d - 1] = set_bit(S[d - 1], k - 2, 0)
            for (i in 0 until d) {
                xor_s = S[i][k - 2].toString().toInt() xor xor_s
                xor_k = K[0][i][k - 2].toString().toInt() xor xor_k
            }
            S[d - 1] = set_bit(S[d - 1], k - 2, 1 xor xor_k xor xor_s)

            xor_s = 0
            xor_k = 0
            S[d - 1] = set_bit(S[d - 1], k - 1, 0)
            for (i in 0 until d) {
                xor_s = S[i][k - 1].toString().toInt() xor xor_s
                xor_k = K[0][i][k - 1].toString().toInt() xor xor_k
            }
            S[d - 1] = set_bit(S[d - 1], k - 1, 1 xor xor_k xor xor_s)

            for (i in 0 until d) {
                for (l in 0..1) {
                    Kp[l][i] = bitwise_rotation(
                        K[l][i].reversed().toBigInteger(2).xor(S[i].reversed().toBigInteger(2))
                            .toString(2), u
                    )
                    var v = Kp[l][i].toString(2)
                    while (v.length < 256)
                        v = "0${v}"
                    Log.d("abs", v)
                }

            }
        }

        fun set_bit(b: String, index: Int, value: Int = Random.nextBits(1)): String {
            return StringBuilder(b).also { it.setCharAt(index, (value + 48).toChar()) }.toString()
        }

        fun generate_big_Int(bits: Int): String {
            var number = "1"
            for (i in 1 until bits) {
                number = Random.nextBits(1).toString() + number
            }
            return number
        }

        fun generate_big_Int(modulus: BigInteger): BigInteger {
            val length = Random.nextInt(0, modulus.toString(2).length)
            val number = "1".toMutableList()
            for (i in 1..length) {
                number.add(Random.nextBits(1).toChar())
            }
            return number.toString().toBigInteger(2)
        }

        fun analize_answer(answer: BigInteger): String {
            var answer_binary = answer.toString(2)
            while (answer_binary.length < 256)
                answer_binary = "0${answer_binary}"
            var best = 0
            var current = 0
            var index = 0

            for (i in 0 until answer_binary.length) {
                if (answer_binary[i] == '0')
                    current++
                else {
                    if (current > best) {
                        best = current
                        index = i
                        Log.d("curret", current.toString())
                        Log.d("best", best.toString())
                        Log.d("index", index.toString())
                    }
                    current = 0
                }
            }
            if (current > 0) {
                for (i in 0 until answer_binary.length) {
                    if (answer_binary[i] == '0')
                        current++
                    else {
                        if (current > best) {
                            best = current
                            index = i
                            Log.d("curret", current.toString())
                            Log.d("best", best.toString())
                            Log.d("index", index.toString())
                        }
                        current = 0
                    }
                }
            }
            Log.d("answer", answer_binary)
            Log.d("answer", answer_binary[index + 1].toString())
            if(answer_binary[index + 1] == '1') {
                return "1"
            } else {
                return "0"
            }
            return answer_binary
        }

        fun bitwise_rotation(x: String, t: Int): BigInteger {
            var s1 = x
            while (s1.length < 256)
                s1 = "0${s1}"
            var s = s1.toMutableList()
            for (i in 0 until t) {
                val tmp = s[0]
                for (j in 1 until s.size) {
                    s[j - 1] = s[j]
                }
                s[s.size - 1] = tmp
            }
            return String(s.toCharArray()).toBigInteger(2)
        }
    }

    class ObliviousTransferSender {
        val length_of_key = 256
        val key_pair = generate_rsa()
        val public_key = key_pair.public as RSAPublicKey
        val private_key = key_pair.private as RSAPrivateKey
        val modulus = public_key.modulus
        val public_exponent = public_key.publicExponent.toInt()
        val private_exponent = private_key.privateExponent
        lateinit var k0: BigInteger
        lateinit var k1: BigInteger
        lateinit var x0: BigInteger
        lateinit var x1: BigInteger
        lateinit var v: BigInteger
        lateinit var mp0: BigInteger
        lateinit var mp1: BigInteger


        fun generate_rsa(): KeyPair {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
            generator.initialize(length_of_key, SecureRandom())
            return generator.genKeyPair()
        }

        fun isVInitialized(): Boolean {
            return this::v.isInitialized
        }

        fun set_v(v: BigInteger) {
            this.v = v
        }

        fun generate_big_Int(bits: Int): BigInteger {
            val number = "1".toMutableList()
            for (i in 1..bits) {
                number.add((Random.nextBits(1) + 48).toChar())
            }
            return String(number.toCharArray()).toBigInteger(2)
        }

        fun generate_xs() {
            x0 = generate_big_Int(length_of_key)
            x1 = generate_big_Int(length_of_key)
        }

        fun generate_ks() {
            k0 = ((v - x0).mod(modulus)).modPow(private_exponent, modulus)
            k1 = ((v - x1).mod(modulus)).modPow(private_exponent, modulus)
        }

        fun generate_ms(m0: BigInteger, m1: BigInteger) {
            mp0 = m0 + k0
            mp1 = m1 + k1
        }
    }

    class ObliviousTransferReceiver {
        lateinit var modulus: BigInteger
        lateinit var public_exponent: BigInteger
        lateinit var x0: BigInteger
        lateinit var x1: BigInteger
        lateinit var v: BigInteger
        lateinit var mp0: BigInteger
        lateinit var mp1: BigInteger
        lateinit var k: BigInteger

        fun set_modulus_exponent(modulus: BigInteger, exponent: BigInteger) {
            this.modulus = modulus
            this.public_exponent = exponent
        }

        fun set_messages(x0: BigInteger, x1: BigInteger) {
            this.x0 = x0
            this.x1 = x1
        }

        fun generate_big_Int(modulus: BigInteger): BigInteger {
            val length = Random.nextInt(0, modulus.toString(2).length)
            val number = "1".toMutableList()
            for (i in 1..length) {
                number.add((Random.nextBits(1) + 48).toChar())
            }
            return String(number.toCharArray()).toBigInteger(2)
        }

        fun generate_v(b: Int) {
            val xb = if (b == 1) x1 else x0
            k = generate_big_Int(modulus)
            while (!k.gcd(modulus).equals(BigInteger.ONE))
                k = generate_big_Int(modulus)
            v = (xb + k.modPow(public_exponent, modulus)).mod(modulus)
        }

        fun set_ms(m0: BigInteger, m1: BigInteger) {
            mp0 = m0
            mp1 = m1
        }

        fun unblind(b: Int): BigInteger {
            return (if (b == 1) mp1 else mp0) - k
        }
    }

    class ObliviousTransfer {
        val length_of_key = 256

        fun generate_rsa(): KeyPair {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
            generator.initialize(length_of_key, SecureRandom())
            return generator.genKeyPair()
        }
    }
}