(function() {
    'use strict';
    var OBJ = {
        "surfFrom": "Web",
        "productName": "ZFJ"
    };
    var Config = {
        DEBUG: false,
        END_POINT: "https://data-qa.getzephyr.com/analytics",
        // END_POINT: "http://localhost:8080/analytics",
        PUBLIC_KEY: "-----BEGIN PUBLIC KEY-----\n" +
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQClG+hv84yotAXSznbzOFJKhjpo\n" +
        "UzS8DJ3DO6Gol6cT+j6Jg3Kj2fH+cJKOJyXyu7pHvu0IllmsXJdUlJP4e6jSkFFy\n" +
        "o8Ju0OhLxgMDu+5xAGyEASOwhBU0RXLLTFF56pHPmJfl3HVPgxLpxpPFASUcQUHA\n" +
        "8Uk8NPkZATKKGl5YzQIDAQAB\n" +
        "-----END PUBLIC KEY-----"
    };

    var win;
    if (typeof(window) === 'undefined') {
        win = {
            navigator: {
                userAgent: ''
            },
            document: {
                location: {}
            }
        };
    } else {
        win = window;
    }

    var USE_XHR = (win.XMLHttpRequest && 'withCredentials' in new XMLHttpRequest());
    var userAgent, user_agent = userAgent = navigator.userAgent;
    var HTTP_PROTOCOL = (('https:' === document.location.protocol) ? 'https://' : 'http://');
    var ENQUEUE_REQUESTS = !USE_XHR && (userAgent.indexOf('MSIE') === -1) && (userAgent.indexOf('Mozilla') === -1);


    var za_ = {
        ipInfoData: false,
        userInfoData: false,
        init: init,
        /**
         * Track event data and store into analytics
         * @param trackObj
         * @param callback
         */
        ipInfoCallback: function(res) {
            OBJ.country = res.country_name;
            OBJ.city = res.city;
            OBJ.remoteAddress = res.ip;
            OBJ.region = res.region_name;
            OBJ.latitude = res.latitude;
            OBJ.longitude = res.longitude;
        },

        userInfoCallback: function(resObj, trackObj, callback) {
            var _ = this;

            OBJ.browser = _.browser();
            OBJ.browserVersion = _.browserVersion();
            OBJ.operatingSystem = _.operatingSystem();
            OBJ.eventTime = _.formatDate(new Date());
            OBJ.screenHeight = screen.height;
            OBJ.screenWidth = screen.width;
            OBJ.url = window.location.toString();
            OBJ.referrer = document.referrer;

            var data = Object.assign({},OBJ, trackObj, resObj);
            var salt = CryptoJS.lib.WordArray.random(16);
            var iv= CryptoJS.lib.WordArray.random(16);
            var encryptedData = CryptoJS.AES.encrypt(JSON.stringify(data), salt, { iv: iv });
            var encryptKey = iv.toString(CryptoJS.enc.Base64)+":"+salt.toString(CryptoJS.enc.Base64);
            var payload = {
                'data': encryptedData.ciphertext.toString()
            };

            var encrypt = new JSEncrypt();
            encrypt.setPublicKey(Config.PUBLIC_KEY);
            var rsaEncryptedKey = encrypt.encrypt(encryptKey);
            payload.key = rsaEncryptedKey;

            //Check if event is there with min length 2
            if (data.event == undefined || data.event && data.event.length<=1){
                return false;
            }

            var analyticUrl = window.analyticUrlFieldValue || document.getElementById('analyticUrlField').value;
            var elChk = document.getElementById('analyticsEnabled');
            var analyticsEnabled = elChk != null ? elChk.value: 'false';
            Config.END_POINT = (analyticUrl != null && analyticUrl.indexOf("https://") !== -1) ? analyticUrl + "/analytics" : Config.END_POINT;

            if (window.navigator && window.navigator.onLine && analyticsEnabled == 'true') { //check analytics enabled and online status
                var xhr = new XMLHttpRequest();
                xhr.open("POST", Config.END_POINT);
                xhr.setRequestHeader("Content-Type", "application/json");
                xhr.send(JSON.stringify(payload))
                xhr.onreadystatechange = function () {
                    if (this.readyState == 4 && this.status == 200) {
                        if (callback && typeof callback === 'function') {
                            try {
                                xhr.responseText ? callback(JSON.parse(xhr.responseText)) : callback(null);
                            } catch (err) {
                                //
                            }
                        }
                    }
                };
            }
        },

        track: function (trackObj, callback) {
            var _ = this;
            var execute = function () {
                if(_.ipInfoData && _.userInfoData) {
                    _.ipInfoCallback(_.ipInfoData);
                    _.userInfoCallback(_.userInfoData, trackObj, callback);
                } else {
                    _.ipInfo(function (ipInfoRes) {
                        _.ipInfoData = ipInfoRes;
                        _.ipInfoCallback(ipInfoRes);
                        _.userInfo(trackObj, function (userInfoRes) {
                            _.userInfoData = userInfoRes;
                            _.userInfoCallback(userInfoRes, trackObj, callback);
                        });
                    });
                }
            };
            setTimeout(execute,1000);

        },

        /**
         * Get user information along with license info
         * @param callback
         */
        userInfo: function (to, callback) {
            var event = to.event != undefined ? to.event : "",
                projectId = to.projectId != undefined ? to.projectId : "",
                versionId = to.versionId != undefined ? to.versionId : "",
                cycleId = to.cycleId != undefined ? to.cycleId : "";
            var url = contextPath + "/rest/zephyr/latest/analytics?event="+
                event+'&projectId='+projectId+'&versionId='+
                versionId+'&cycleId='+cycleId;
            AJS.$.get(url, function(data, status){
                if (status=='success') {
                    return callback(data);
                }
            });
        },

        /**
         * Trim space from string.
         * @param str
         * @returns {XML|string|void}
         */
        trim :  function(str) {
            return str.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '');
        },


        /**
         * Generate UUID
         * @returns {string}
         * @private
         */
        UUID : function() {
            var s = [];var hexDigits = "0123456789abcdef";
            for (var i = 0; i < 36; i++) {
                s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
            }
            s[14] = "4";s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
            s[8] = s[13] = s[18] = s[23] = "-";var uuid = s.join("");
            return uuid;
        },

        /**
         * Get User IP address by using WebRTC
         */
        getUserIP : function(onNewIP) {
            var myPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
            if(!myPeerConnection) {
                if(onNewIP && typeof onNewIP === 'function') {
                    onNewIP('');
                }
                return;
            }
            var pc = new myPeerConnection({
                    iceServers: []
                }),
                noop = function() {},
                localIPs = {},
                ipRegex = /([0-9]{1,3}(\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(:[a-f0-9]{1,4}){7})/g,
                key;

            function iterateIP(ip) {
                if (!localIPs[ip] && onNewIP != undefined) onNewIP(ip);
                localIPs[ip] = true;
            }

            if(pc && pc.createDataChannel && typeof pc.createDataChannel === 'function') {
                pc.createDataChannel("");
            }
            pc.createOffer().then(function(sdp) {
                sdp.sdp.split('\n').forEach(function(line) {
                    if (line.indexOf('candidate') < 0) return;
                    line.match(ipRegex).forEach(iterateIP);
                });
                pc.setLocalDescription(sdp, noop, noop);
            });

            pc.onicecandidate = function(ice) {
                if (!ice || !ice.candidate || !ice.candidate.candidate || !ice.candidate.candidate.match(ipRegex)) return;
                ice.candidate.candidate.match(ipRegex).forEach(iterateIP);
            };
        },
        /**
         * Check string part exist
         * @param str
         * @param needle
         * @returns {boolean}
         */
        includes : function(str, needle) {
            return str.indexOf(needle) !== -1;
        },

        /**
         * Get browser name from navigation.userAgent
         * @param vendor
         * @param opera
         * @returns {*}
         */
        browser : function(vendor, opera) {
            var that = this;
            vendor = vendor || '';
            if (opera || that.includes(user_agent, ' OPR/')) {
                if (that.includes(user_agent, 'Mini')) {
                    return 'Opera Mini';
                }
                return 'Opera';
            } else if (/(BlackBerry|PlayBook|BB10)/i.test(user_agent)) {
                return 'BlackBerry';
            } else if (that.includes(user_agent, 'IEMobile') || that.includes(user_agent, 'WPDesktop')) {
                return 'Internet Explorer Mobile';
            } else if (that.includes(user_agent, 'Edge')) {
                return 'Microsoft Edge';
            } else if (that.includes(user_agent, 'FBIOS')) {
                return 'Facebook Mobile';
            } else if (that.includes(user_agent, 'Chrome')) {
                return 'Chrome';
            } else if (that.includes(user_agent, 'CriOS')) {
                return 'Chrome iOS';
            } else if (that.includes(user_agent, 'UCWEB') || that.includes(user_agent, 'UCBrowser')) {
                return 'UC Browser';
            } else if (that.includes(user_agent, 'FxiOS')) {
                return 'Firefox iOS';
            } else if (that.includes(vendor, 'Apple')) {
                if (that.includes(user_agent, 'Mobile')) {
                    return 'Mobile Safari';
                }
                return 'Safari';
            } else if (that.includes(user_agent, 'Android')) {
                return 'Android Mobile';
            } else if (that.includes(user_agent, 'Konqueror')) {
                return 'Konqueror';
            } else if (that.includes(user_agent, 'Firefox')) {
                return 'Firefox';
            } else if (that.includes(user_agent, 'MSIE') || that.includes(user_agent, 'Trident/')) {
                return 'Internet Explorer';
            } else if (that.includes(user_agent, 'Gecko')) {
                return 'Mozilla';
            } else {
                return '';
            }
        },

        /**
         * Get browser version by using navigator.userAgent
         */
        browserVersion : function(vendor, opera) {
            var that = this;
            var userAgent = user_agent;
            var browser = that.browser(vendor, opera);
            var versionRegexs = {
                'Internet Explorer Mobile': /rv:(\d+(\.\d+)?)/,
                'Microsoft Edge': /Edge\/(\d+(\.\d+)?)/,
                'Chrome': /Chrome\/(\d+(\.\d+)?)/,
                'Chrome iOS': /CriOS\/(\d+(\.\d+)?)/,
                'UC Browser': /(UCBrowser|UCWEB)\/(\d+(\.\d+)?)/,
                'Safari': /Version\/(\d+(\.\d+)?)/,
                'Mobile Safari': /Version\/(\d+(\.\d+)?)/,
                'Opera': /(Opera|OPR)\/(\d+(\.\d+)?)/,
                'Firefox': /Firefox\/(\d+(\.\d+)?)/,
                'Firefox iOS': /FxiOS\/(\d+(\.\d+)?)/,
                'Konqueror': /Konqueror:(\d+(\.\d+)?)/,
                'BlackBerry': /BlackBerry (\d+(\.\d+)?)/,
                'Android Mobile': /android\s(\d+(\.\d+)?)/,
                'Internet Explorer': /(rv:|MSIE )(\d+(\.\d+)?)/,
                'Mozilla': /rv:(\d+(\.\d+)?)/
            };
            var regex = versionRegexs[browser];
            if (regex === undefined) {
                return null;
            }
            var matches = userAgent.match(regex);
            if (!matches) {
                return null;
            }
            return parseFloat(matches[matches.length - 2]);
        },

        /**
         * Get operating system information
         */
        operatingSystem : function() {
            var that = this;
            var a = user_agent;
            if (/Windows/i.test(a)) {
                if (/Phone/.test(a) || /WPDesktop/.test(a)) {
                    return 'Windows Phone';
                }
                return 'Windows';
            } else if (/(iPhone|iPad|iPod)/.test(a)) {
                return 'iOS';
            } else if (/Android/.test(a)) {
                return 'Android';
            } else if (/(BlackBerry|PlayBook|BB10)/i.test(a)) {
                return 'BlackBerry';
            } else if (/Mac/i.test(a)) {
                return 'Mac OS X';
            } else if (/Linux/.test(a)) {
                return 'Linux';
            } else if (/CrOS/.test(a)) {
                return 'Chrome OS';
            } else {
                return '';
            }
        },

        /**
         * Get operating device name.
         * @returns {*}
         */
        operatingDevice : function() {
            if (/Windows Phone/i.test(user_agent) || /WPDesktop/.test(user_agent)) {
                return 'Windows Phone';
            } else if (/iPad/.test(user_agent)) {
                return 'iPad';
            } else if (/iPod/.test(user_agent)) {
                return 'iPod Touch';
            } else if (/iPhone/.test(user_agent)) {
                return 'iPhone';
            } else if (/(BlackBerry|PlayBook|BB10)/i.test(user_agent)) {
                return 'BlackBerry';
            } else if (/Android/.test(user_agent)) {
                return 'Android';
            } else {
                return '';
            }
        },

        /**
         * Get Referring Domain
         * @param referrer
         * @returns {*}
         */
        referringDomain : function() {
            var referrer = window.location.toString();
            var split = referrer.split('/');
            if (split.length >= 3) {
                return split[2];
            }
            return '';
        },

    ipInfo: function(callback){
        var elChk = document.getElementById('analyticsEnabled');
        var analyticsEnabled = elChk != null ? elChk.value : 'false';
        if (window.navigator && window.navigator.onLine && analyticsEnabled == 'true') { //check online status
            var xhr = new XMLHttpRequest();
            xhr.onreadystatechange = function () {
                if (this.readyState == 4 && this.status == 200) {
                    try {
                        callback(JSON.parse(this.responseText), null, 2);
                    } catch (e) {
                        //console.log(e);
                    }
                }
            };
            var userIP = "";
            if (window.localStorage != undefined) {
                userIP = window.localStorage.getItem('userIP');
                if (userIP == undefined || userIP == null) {
                    userIP = window.location.hostname; //use localIp instead
                }
            }


            xhr.open("GET", "https://geoip.getzephyr.com/json/" + userIP, true);
            xhr.setRequestHeader("Accept", "application/json");
            xhr.send();
        }
    },

        /**
         * Escape HTML for skip XSRF
         * @param s
         * @returns {*}
         */
        escapeHTML : function(s) {
            var that = this;
            var escaped = s;
            if (escaped && that.isString(escaped)) {
                escaped = escaped
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;')
                    .replace(/'/g, '&#039;');
            }
            return escaped;
        },

        /**
         * Get current timestamp
         * @returns {number}
         */
        timestamp : function() {
            Date.now = Date.now || function() {
                return +new Date;
            };
            return Date.now();
        },

        /**
         * Get current formatted date or new one.
         * @param d
         * @returns {string}
         */
        formatDate : function(d) {
            // YYYY-MM-DDTHH:MM:SS in UTC
            function pad(n) {
                return n < 10 ? '0' + n : n;
            }
            return d.getUTCFullYear() + '-' +
                pad(d.getUTCMonth() + 1) + '-' +
                pad(d.getUTCDate()) + 'T' +
                pad(d.getUTCHours()) + ':' +
                pad(d.getUTCMinutes()) + ':' +
                pad(d.getUTCSeconds());
        },

        /**
         * Encode json data.
         */
        JSONEncode : (function() {
            return function(mixed_val) {
                var value = mixed_val;
                var quote = function(string) {
                    var escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g; // eslint-disable-line no-control-regex
                    var meta = {
                        '\b': '\\b',
                        '\t': '\\t',
                        '\n': '\\n',
                        '\f': '\\f',
                        '\r': '\\r',
                        '"': '\\"',
                        '\\': '\\\\'
                    };

                    escapable.lastIndex = 0;
                    return escapable.test(string) ?
                        '"' + string.replace(escapable, function(a) {
                            var c = meta[a];
                            return typeof c === 'string' ? c :
                                '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
                        }) + '"' :
                        '"' + string + '"';
                };

                var str = function(key, holder) {
                    var gap = '';
                    var indent = '    ';
                    var i = 0;
                    var k = '';
                    var v = '';
                    var length = 0;
                    var mind = gap;
                    var partial = [];
                    var value = holder[key];

                    if (value && typeof value === 'object' &&
                        typeof value.toJSON === 'function') {
                        value = value.toJSON(key);
                    }

                    switch (typeof value) {
                        case 'string':
                            return quote(value);

                        case 'number':
                            return isFinite(value) ? String(value) : 'null';

                        case 'boolean':
                        case 'null':
                            return String(value);
                        case 'object':
                            if (!value) {
                                return 'null';
                            }
                            gap += indent;
                            partial = [];

                            if (toString.apply(value) === '[object Array]') {

                                length = value.length;
                                for (i = 0; i < length; i += 1) {
                                    partial[i] = str(i, value) || 'null';
                                }
                                v = partial.length === 0 ? '[]' :
                                    gap ? '[\n' + gap +
                                        partial.join(',\n' + gap) + '\n' +
                                        mind + ']' :
                                        '[' + partial.join(',') + ']';
                                gap = mind;
                                return v;
                            }

                            for (k in value) {
                                if (hasOwnProperty.call(value, k)) {
                                    v = str(k, value);
                                    if (v) {
                                        partial.push(quote(k) + (gap ? ': ' : ':') + v);
                                    }
                                }
                            }

                            v = partial.length === 0 ? '{}' :
                                gap ? '{' + partial.join(',') + '' +
                                    mind + '}' : '{' + partial.join(',') + '}';
                            gap = mind;
                            return v;
                    }
                };
                return str('', {
                    '': value
                });
            };
        })(),


        /**
         * Decode json data.
         */
        JSONDecode : (function() {
            var at,
                ch,
                escapee = {
                    '"': '"',
                    '\\': '\\',
                    '/': '/',
                    'b': '\b',
                    'f': '\f',
                    'n': '\n',
                    'r': '\r',
                    't': '\t'
                },
                text,
                error = function(m) {
                    throw {
                        name: 'SyntaxError',
                        message: m,
                        at: at,
                        text: text
                    };
                },
                next = function(c) {
                    if (c && c !== ch) {
                        error('Expected \'' + c + '\' instead of \'' + ch + '\'');
                    }
                    ch = text.charAt(at);
                    at += 1;
                    return ch;
                },
                number = function() {
                    var number,
                        string = '';

                    if (ch === '-') {
                        string = '-';
                        next('-');
                    }
                    while (ch >= '0' && ch <= '9') {
                        string += ch;
                        next();
                    }
                    if (ch === '.') {
                        string += '.';
                        while (next() && ch >= '0' && ch <= '9') {
                            string += ch;
                        }
                    }
                    if (ch === 'e' || ch === 'E') {
                        string += ch;
                        next();
                        if (ch === '-' || ch === '+') {
                            string += ch;
                            next();
                        }
                        while (ch >= '0' && ch <= '9') {
                            string += ch;
                            next();
                        }
                    }
                    number = +string;
                    if (!isFinite(number)) {
                        error('Bad number');
                    } else {
                        return number;
                    }
                },

                string = function() {
                    var hex,
                        i,
                        string = '',
                        uffff;
                    if (ch === '"') {
                        while (next()) {
                            if (ch === '"') {
                                next();
                                return string;
                            }
                            if (ch === '\\') {
                                next();
                                if (ch === 'u') {
                                    uffff = 0;
                                    for (i = 0; i < 4; i += 1) {
                                        hex = parseInt(next(), 16);
                                        if (!isFinite(hex)) {
                                            break;
                                        }
                                        uffff = uffff * 16 + hex;
                                    }
                                    string += String.fromCharCode(uffff);
                                } else if (typeof escapee[ch] === 'string') {
                                    string += escapee[ch];
                                } else {
                                    break;
                                }
                            } else {
                                string += ch;
                            }
                        }
                    }
                    error('Bad string');
                },
                white = function() {
                    while (ch && ch <= ' ') {
                        next();
                    }
                },
                word = function() {
                    switch (ch) {
                        case 't':
                            next('t');
                            next('r');
                            next('u');
                            next('e');
                            return true;
                        case 'f':
                            next('f');
                            next('a');
                            next('l');
                            next('s');
                            next('e');
                            return false;
                        case 'n':
                            next('n');
                            next('u');
                            next('l');
                            next('l');
                            return null;
                    }
                    error('Unexpected "' + ch + '"');
                },
                value,
                array = function() {
                    var array = [];
                    if (ch === '[') {
                        next('[');
                        white();
                        if (ch === ']') {
                            next(']');
                            return array;
                        }
                        while (ch) {
                            array.push(value());
                            white();
                            if (ch === ']') {
                                next(']');
                                return array;
                            }
                            next(',');
                            white();
                        }
                    }
                    error('Bad array');
                },
                object = function() {
                    var key,
                        object = {};

                    if (ch === '{') {
                        next('{');
                        white();
                        if (ch === '}') {
                            next('}');
                            return object;
                        }
                        while (ch) {
                            key = string();
                            white();
                            next(':');
                            if (Object.hasOwnProperty.call(object, key)) {
                                error('Duplicate key "' + key + '"');
                            }
                            object[key] = value();
                            white();
                            if (ch === '}') {
                                next('}');
                                return object;
                            }
                            next(',');
                            white();
                        }
                    }
                    error('Bad object');
                };

            value = function() {
                white();
                switch (ch) {
                    case '{':
                        return object();
                    case '[':
                        return array();
                    case '"':
                        return string();
                    case '-':
                        return number();
                    default:
                        return ch >= '0' && ch <= '9' ? number() : word();
                }
            };

            return function(source) {
                var result;

                text = source;
                at = 0;
                ch = ' ';
                result = value();
                white();
                if (ch) {
                    error('Syntax error');
                }

                return result;
            };
        })(),

        /**
         * Encode string to base64
         * @param data
         * @returns {*}
         */
        base64Encode : function(data) {
            var that = this;
            var b64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
            var o1, o2, o3, h1, h2, h3, h4, bits, i = 0,
                ac = 0,
                enc = '',
                tmp_arr = [];

            if (!data) {
                return data;
            }

            data = that.utf8Encode(data);

            do {
                o1 = data.charCodeAt(i++);
                o2 = data.charCodeAt(i++);
                o3 = data.charCodeAt(i++);

                bits = o1 << 16 | o2 << 8 | o3;

                h1 = bits >> 18 & 0x3f;
                h2 = bits >> 12 & 0x3f;
                h3 = bits >> 6 & 0x3f;
                h4 = bits & 0x3f;

                tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
            } while (i < data.length);

            enc = tmp_arr.join('');

            switch (data.length % 3) {
                case 1:
                    enc = enc.slice(0, -2) + '==';
                    break;
                case 2:
                    enc = enc.slice(0, -1) + '=';
                    break;
            }

            return enc;
        },

        /**
         * Encode utf-8 string.
         * @param string
         * @returns {string}
         */
        utf8Encode : function(string) {
            string = (string + '').replace(/\r\n/g, '\n').replace(/\r/g, '\n');

            var utftext = '',
                start,
                end;
            var stringl = 0,
                n;

            start = end = 0;
            stringl = string.length;

            for (n = 0; n < stringl; n++) {
                var c1 = string.charCodeAt(n);
                var enc = null;

                if (c1 < 128) {
                    end++;
                } else if ((c1 > 127) && (c1 < 2048)) {
                    enc = String.fromCharCode((c1 >> 6) | 192, (c1 & 63) | 128);
                } else {
                    enc = String.fromCharCode((c1 >> 12) | 224, ((c1 >> 6) & 63) | 128, (c1 & 63) | 128);
                }
                if (enc !== null) {
                    if (end > start) {
                        utftext += string.substring(start, end);
                    }
                    utftext += enc;
                    start = end = n + 1;
                }
            }

            if (end > start) {
                utftext += string.substring(start, string.length);
            }

            return utftext;
        },

        /**
         * Get query parameter from current url with name.
         * @param url
         * @param param
         * @returns {*}
         */
        getQueryParam : function(url, param) {
            // Expects a raw URL

            param = param.replace(/[\[]/, '\\\[').replace(/[\]]/, '\\\]');
            var regexS = '[\\?&]' + param + '=([^&#]*)',
                regex = new RegExp(regexS),
                results = regex.exec(url);
            if (results === null || (results && typeof(results[1]) !== 'string' && results[1].length)) {
                return '';
            } else {
                return decodeURIComponent(results[1]).replace(/\+/g, ' ');
            }
        },

        /**
         * Get hash parameter.
         * @param hash
         * @param param
         * @returns {null}
         */
        getHashParam : function(hash, param) {
            var matches = hash.match(new RegExp(param + '=([^&]*)'));
            return matches ? matches[1] : null;
        },

        /**
         * Get and set cookie and remove.
         * @type {{get: get, parse: parse, set_seconds: set_seconds, set: set, remove: remove}}
         */
        cookie : {
            get: function(name) {
                var nameEQ = 'za_'+name + '=';
                var ca = document.cookie.split(';');
                for (var i = 0; i < ca.length; i++) {
                    var c = ca[i];
                    while (c.charAt(0) == ' ') {
                        c = c.substring(1, c.length);
                    }
                    if (c.indexOf(nameEQ) === 0) {
                        return decodeURIComponent(c.substring(nameEQ.length, c.length));
                    }
                }
                return null;
            },

            parse: function(name) {
                var cookie;
                try {
                    cookie = this.JSONDecode(za_.cookie.get(name)) || {};
                } catch (err) {
                }
                return cookie;
            },

            set_seconds: function(name, value, seconds, cross_subdomain, is_secure) {
                var cdomain = '',
                    expires = '',
                    secure = '';

                if (cross_subdomain) {
                    var matches = document.location.hostname.match(/[a-z0-9][a-z0-9\-]+\.[a-z\.]{2,6}$/i),
                        domain = matches ? matches[0] : '';

                    cdomain = ((domain) ? '; domain=.' + domain : '');
                }

                if (seconds) {
                    var date = new Date();
                    date.setTime(date.getTime() + (seconds * 1000));
                    expires = '; expires=' + date.toGMTString();
                }

                if (is_secure) {
                    secure = '; secure';
                }

                document.cookie = name + '=' + encodeURIComponent(value) + expires + '; path=/' + cdomain + secure;
            },

            set: function(name, value, days, cross_subdomain, is_secure) {
                var cdomain = '',
                    expires = '',
                    secure = '';

                if (cross_subdomain) {
                    var matches = document.location.hostname.match(/[a-z0-9][a-z0-9\-]+\.[a-z\.]{2,6}$/i),
                        domain = matches ? matches[0] : '';

                    cdomain = ((domain) ? '; domain=.' + domain : '');
                }

                if (days) {
                    var date = new Date();
                    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
                    expires = '; expires=' + date.toGMTString();
                }

                if (is_secure) {
                    secure = '; secure';
                }

                var new_cookie_val = 'za_'+name + '=' + encodeURIComponent(value) + expires + '; path=/' + cdomain + secure;
                document.cookie = new_cookie_val;
                return new_cookie_val;
            },

            remove: function(name, cross_subdomain) {
                za_.cookie.set(name, '', -1, cross_subdomain);
            }
        },

        /**
         * Get, Set, Remove and Parse LocalStorage
         * @type {{error: error, get: get, parse: parse, set: set, remove: remove}}
         */
        localStorage : {
            error: function(msg) {
                console.error('localStorage error: ' + msg);
            },

            get: function(name) {
                try {
                    return window.localStorage.getItem(name);
                } catch (err) {
                    za_.localStorage.error(err);
                }
                return null;
            },

            parse: function(name) {
                try {
                    return za_.JSONDecode(za_.localStorage.get(name)) || {};
                } catch (err) {
                    za_.localStorage.error(err);
                }
                return null;
            },

            set: function(name, value) {
                try {
                    window.localStorage.setItem(name, value);
                } catch (err) {
                    za_.localStorage.error(err);
                }
            },

            remove: function(name) {
                try {
                    window.localStorage.removeItem(name);
                } catch (err) {
                    za_.localStorage.error(err);
                }
            }
        }
    };

    /**
     * ZephyrAnalytics Persistence Object
     * @constructor
     */
    var ZAPersistence = function(config) {
        this['props'] = {};

        if (config['persistence']) {
            this.name = 'za_' + config['persistence'];
        } else {
            this.name = 'za_' + config['token'] + '_zephyr';
        }

        var storage_type = config['persistence'];
        if (storage_type !== 'cookie' && storage_type !== 'localStorage') {
            console.error('Unknown persistence type ' + storage_type + '; falling back to cookie');
            storage_type = config['persistence'] = 'cookie';
        }

        var localStorage_supported = function() {
            var supported = true;
            try {
                var key = '__zasupport__',
                    val = 'xyz';
                za_.localStorage.set(key, val);
                if (za_.localStorage.get(key) !== val) {
                    supported = false;
                }
                za_.localStorage.remove(key);
            } catch (err) {
                supported = false;
            }
            if (!supported) {
                console.error('Going back to cookie storage because localStorage unsupported');
            }
            return supported;
        };
        if (storage_type === 'localStorage' && localStorage_supported()) {
            this.storage = za_.localStorage;
        } else {
            this.storage = za_.cookie;
        }

        this.load();
        this.update_config(config);
        this.upgrade(config);
        this.save();
    };

    ZAPersistence.prototype.properties = function() {
        var p = {};
        za_.each(this['props'], function(v, k) {
            if (!za_.include(RESERVED_PROPERTIES, k)) {
                p[k] = v;
            }
        });
        return p;
    };

    ZAPersistence.prototype.load = function() {
        if (this.disabled) {
            return;
        }

        var entry = this.storage.parse(this.name);

        if (entry) {
            this['props'] = za_.extend({}, entry);
        }
    };

    ZAPersistence.prototype.upgrade = function(config) {
        var upgrade_from_old_lib = config['upgrade'],
            old_cookie_name,
            old_cookie;
        if (upgrade_from_old_lib) {
            old_cookie_name = 'za_old_properties';
            if (typeof(upgrade_from_old_lib) === 'string') {
                old_cookie_name = upgrade_from_old_lib;
            }
            old_cookie = this.storage.parse(old_cookie_name);
            this.storage.remove(old_cookie_name);
            this.storage.remove(old_cookie_name, true);
            if (old_cookie) {
                this['props'] = za_.extend(
                    this['props'],
                    old_cookie['all'],
                    old_cookie['events']
                );
            }
        }

        if (!config['cookie_name'] && config['name'] !== 'zephyranalytic') {
            old_cookie_name = 'za_' + config['token'] + '_' + config['name'];
            old_cookie = this.storage.parse(old_cookie_name);

            if (old_cookie) {
                this.storage.remove(old_cookie_name);
                this.storage.remove(old_cookie_name, true);
                this.register_once(old_cookie);
            }
        }

        if (this.storage === za_.localStorage) {
            old_cookie = za_.cookie.parse(this.name);

            za_.cookie.remove(this.name);
            za_.cookie.remove(this.name, true);

            if (old_cookie) {
                this.register_once(old_cookie);
            }
        }
    };

    ZAPersistence.prototype.save = function() {
        if (this.disabled) {
            return;
        }
        this.storage.set(
            this.name,
            za_.JSONEncode(this['props']),
            this.expire_days
        );
    };

    ZAPersistence.prototype.register_once = function(props, default_value, days) {
        if (za_.isObject(props)) {
            if (typeof(default_value) === 'undefined') {
                default_value = 'None';
            }
            this.expire_days = (typeof(days) === 'undefined') ? this.default_expiry : days;

            za_.each(props, function(val, prop) {
                if (!this['props'].hasOwnProperty(prop) || this['props'][prop] === default_value) {
                    this['props'][prop] = val;
                }
            }, this);

            this.save();

            return true;
        }
        return false;
    };


    ZAPersistence.prototype.register = function(props, days) {
        if (za_.isObject(props)) {
            this.expire_days = (typeof(days) === 'undefined') ? this.default_expiry : days;

            za_.extend(this['props'], props);

            this.save();

            return true;
        }
        return false;
    };

    ZAPersistence.prototype.set_cross_subdomain = function(cross_subdomain) {
        if (cross_subdomain !== this.cross_subdomain) {
            this.cross_subdomain = cross_subdomain;
            this.remove();
            this.save();
        }
    };

    ZAPersistence.prototype.unregister = function(prop) {
        if (prop in this['props']) {
            delete this['props'][prop];
            this.save();
        }
    };

    ZAPersistence.prototype.update_config = function(config) {
        this.default_expiry = this.expire_days = config['cookie_expiration'];
        this.set_disabled(config['disable_persistence']);
        this.set_cross_subdomain(config['cross_subdomain_cookie']);
    };

    ZAPersistence.prototype.set_disabled = function(disabled) {
        this.disabled = disabled;
        if (this.disabled) {
            this.remove();
        }
    };

    var ZephyrEvents = {
        'CREATE_CYCLE':'Create Cycle',
        'UPDATE_CYCLE':'Update Cycle',
        'DELETE_CYCLE': 'Delete Cycle',
        'CLONE_CYCLE': 'Clone Cycle',
        'MOVE_CYCLE': 'Move Cycle',
        'EXPORT_CYCLE': 'Export Cycle',
        'SEARCH_CYCLE': 'Search Cycle',
        'COLLAPSE_EXPAND_CYCLE': 'Collapse or Expand Cycle',
        'ADD_TEST_TO_CYCLE': 'Add Test to Cycle',
        'ADD_FOLDER': 'Add Folder',
        'UPDATE_FOLDER':'Update Folder',
        'DELETE_FOLDER':'Delete Folder',
        'CLONE_FOLDER':'Clone Folder',
        'EXPORT_FOLDER':'Export Folder',
        'EXECUTE': 'Update Execution Status',
        'ADD_EXECUTION': 'Add Execution',
        'UPDATE_EXECUTION': 'Update Execution',
        'DELETE_EXECUTION': 'Delete Execution',
        'SEARCH_TEST_EXECUTION': 'Search Test Execution',
        'DELETE_BULK_EXECUTION': 'Delete Bulk Execution',
        'BULK_ASSOCIATE_DEFECTS': 'Bulk Associate Defect',
        'ADD_ASSIGNEE_EXECUTION': 'Add Assignee to Execution',
        'ADD_ASSIGNEE_BULK_EXECUTION': 'Add Assignee to Bulk Execution',
        'COPY_MOVE_BULK_EXECUTION': 'Copy/Move Bulk Execution',
        'UPDATE_BULK_EXECUTION_STATUS': 'Update Bulk Execution Status',
        'ADD_TEST_STEP': 'Add Test Step',
        'UPDATE_TEST_STEP': 'Update Test Step',
        'DELETE_TEST_STEP': 'Delete Test Step',
        'MOVE_TEST_STEP': 'Move Test Step',
        'ADD_TEST_STEP_ATTACHMENT': 'Add Test Step Attachment',
        'DELETE_TEST_STEP_ATTACHMENT': 'Delete Test Step Attachment',
        'ADD_EXECUTION_ATTACHMENT': 'Add Execution Attachment',
        'DELETE_EXECUTION_ATTACHMENT': 'Delete Execution Attachment',
        'ADD_TEST_STEP_DEFECT':'Add Test Step Defect',
        'ADD_EXECUTION_DEFECT': 'Add Execution Defect',
        'ADD_EXECUTION_ASSIGNEE': 'Add Execution Assignee',
        'EXECUTION_LIST_VIEW': 'Execution List View',
        'EXECUTION_DETAILS_VIEW': 'Execution Details View',
        'HOVER_EXECUTION_DEFECTS': 'Hover Execution Defects',
        'SEARCH_TEST_SUMMARY_VERSIONS': 'Search Test Summary Versions',
        'SEARCH_TEST_SUMMARY_COMPONENTS': 'Search Test Summary Components',
        'SEARCH_TEST_SUMMARY_LABELS': 'Search Test Summary Labels',
        'SEARCH_TRACEABILITY_REPORTS': 'Search Traceability Report',
        'IN_APP_DISTRIBUTION_CLICK': 'In App Distribution click',
        'IN_APP_OPT_OUT': 'In App Opted out'
    };

    function init(config) {
        //do some basic stuff
        var _ = this;
        //set userIP into localStorage
        _.getUserIP(function (res) {
            if(window.localStorage != undefined){
                window.localStorage.setItem('userIP',res)
            }
        });
        _.ipInfo(function (ipInfoRes) {
            _.ipInfoData = ipInfoRes;
            _.userInfo(OBJ, function (userInfoRes) {
                _.userInfoData = userInfoRes;
            });
        });
    }



    //bind into windows
    window.za = za_;
    window.ZephyrEvents = ZephyrEvents;
    document.addEventListener("DOMContentLoaded", function(event) {
        za_.init();
    });

    return za;
}());