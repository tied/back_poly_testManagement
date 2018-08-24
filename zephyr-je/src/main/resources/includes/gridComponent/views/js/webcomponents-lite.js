/**
@license @nocompile
Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
Code distributed by Google as part of the polymer project is also
subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
*/
(function() {
    /*

     Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
     This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
     The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
     The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
     Code distributed by Google as part of the polymer project is also
     subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
    */
    'use strict';
    var p, q = "undefined" != typeof window && window === this ? this : "undefined" != typeof global && null != global ? global : this,
        aa = "function" == typeof Object.defineProperties ? Object.defineProperty : function(a, b, c) {
            a != Array.prototype && a != Object.prototype && (a[b] = c.value)
        };

    function da() {
        da = function() {};
        q.Symbol || (q.Symbol = ea)
    }
    var ea = function() {
        var a = 0;
        return function(b) {
            return "jscomp_symbol_" + (b || "") + a++
        }
    }();

    function fa() {
        da();
        var a = q.Symbol.iterator;
        a || (a = q.Symbol.iterator = q.Symbol("iterator"));
        "function" != typeof Array.prototype[a] && aa(Array.prototype, a, {
            configurable: !0,
            writable: !0,
            value: function() {
                return ha(this)
            }
        });
        fa = function() {}
    }

    function ha(a) {
        var b = 0;
        return ia(function() {
            return b < a.length ? {
                done: !1,
                value: a[b++]
            } : {
                done: !0
            }
        })
    }

    function ia(a) {
        fa();
        a = {
            next: a
        };
        a[q.Symbol.iterator] = function() {
            return this
        };
        return a
    }

    function la(a) {
        fa();
        var b = a[Symbol.iterator];
        return b ? b.call(a) : ha(a)
    }

    function ma(a) {
        for (var b, c = []; !(b = a.next()).done;) c.push(b.value);
        return c
    }
    (function() {
        if (! function() {
                var a = document.createEvent("Event");
                a.initEvent("foo", !0, !0);
                a.preventDefault();
                return a.defaultPrevented
            }()) {
            var a = Event.prototype.preventDefault;
            Event.prototype.preventDefault = function() {
                this.cancelable && (a.call(this), Object.defineProperty(this, "defaultPrevented", {
                    get: function() {
                        return !0
                    },
                    configurable: !0
                }))
            }
        }
        var b = /Trident/.test(navigator.userAgent);
        if (!window.CustomEvent || b && "function" !== typeof window.CustomEvent) window.CustomEvent = function(a, b) {
            b = b || {};
            var c = document.createEvent("CustomEvent");
            c.initCustomEvent(a, !!b.bubbles, !!b.cancelable, b.detail);
            return c
        }, window.CustomEvent.prototype = window.Event.prototype;
        if (!window.Event || b && "function" !== typeof window.Event) {
            var c = window.Event;
            window.Event = function(a, b) {
                b = b || {};
                var c = document.createEvent("Event");
                c.initEvent(a, !!b.bubbles, !!b.cancelable);
                return c
            };
            if (c)
                for (var d in c) window.Event[d] = c[d];
            window.Event.prototype = c.prototype
        }
        if (!window.MouseEvent || b && "function" !== typeof window.MouseEvent) {
            b = window.MouseEvent;
            window.MouseEvent = function(a,
                b) {
                b = b || {};
                var c = document.createEvent("MouseEvent");
                c.initMouseEvent(a, !!b.bubbles, !!b.cancelable, b.view || window, b.detail, b.screenX, b.screenY, b.clientX, b.clientY, b.ctrlKey, b.altKey, b.shiftKey, b.metaKey, b.button, b.relatedTarget);
                return c
            };
            if (b)
                for (d in b) window.MouseEvent[d] = b[d];
            window.MouseEvent.prototype = b.prototype
        }
        Array.from || (Array.from = function(a) {
            return [].slice.call(a)
        });
        Object.assign || (Object.assign = function(a, b) {
            for (var c = [].slice.call(arguments, 1), d = 0, e; d < c.length; d++)
                if (e = c[d])
                    for (var f =
                            a, m = e, n = Object.getOwnPropertyNames(m), t = 0; t < n.length; t++) e = n[t], f[e] = m[e];
            return a
        })
    })(window.WebComponents);
    (function() {
        function a() {}

        function b(a, b) {
            if (!a.childNodes.length) return [];
            switch (a.nodeType) {
                case Node.DOCUMENT_NODE:
                    return t.call(a, b);
                case Node.DOCUMENT_FRAGMENT_NODE:
                    return B.call(a, b);
                default:
                    return n.call(a, b)
            }
        }
        var c = "undefined" === typeof HTMLTemplateElement,
            d = !(document.createDocumentFragment().cloneNode() instanceof DocumentFragment),
            e = !1;
        /Trident/.test(navigator.userAgent) && function() {
            function a(a, b) {
                if (a instanceof DocumentFragment)
                    for (var d; d = a.firstChild;) c.call(this, d, b);
                else c.call(this,
                    a, b);
                return a
            }
            e = !0;
            var b = Node.prototype.cloneNode;
            Node.prototype.cloneNode = function(a) {
                a = b.call(this, a);
                this instanceof DocumentFragment && (a.__proto__ = DocumentFragment.prototype);
                return a
            };
            DocumentFragment.prototype.querySelectorAll = HTMLElement.prototype.querySelectorAll;
            DocumentFragment.prototype.querySelector = HTMLElement.prototype.querySelector;
            Object.defineProperties(DocumentFragment.prototype, {
                nodeType: {
                    get: function() {
                        return Node.DOCUMENT_FRAGMENT_NODE
                    },
                    configurable: !0
                },
                localName: {
                    get: function() {},
                    configurable: !0
                },
                nodeName: {
                    get: function() {
                        return "#document-fragment"
                    },
                    configurable: !0
                }
            });
            var c = Node.prototype.insertBefore;
            Node.prototype.insertBefore = a;
            var d = Node.prototype.appendChild;
            Node.prototype.appendChild = function(b) {
                b instanceof DocumentFragment ? a.call(this, b, null) : d.call(this, b);
                return b
            };
            var f = Node.prototype.removeChild,
                h = Node.prototype.replaceChild;
            Node.prototype.replaceChild = function(b, c) {
                b instanceof DocumentFragment ? (a.call(this, b, c), f.call(this, c)) : h.call(this, b, c);
                return c
            };
            Document.prototype.createDocumentFragment =
                function() {
                    var a = this.createElement("df");
                    a.__proto__ = DocumentFragment.prototype;
                    return a
                };
            var g = Document.prototype.importNode;
            Document.prototype.importNode = function(a, b) {
                b = g.call(this, a, b || !1);
                a instanceof DocumentFragment && (b.__proto__ = DocumentFragment.prototype);
                return b
            }
        }();
        var f = Node.prototype.cloneNode,
            h = Document.prototype.createElement,
            g = Document.prototype.importNode,
            k = Node.prototype.removeChild,
            l = Node.prototype.appendChild,
            m = Node.prototype.replaceChild,
            n = Element.prototype.querySelectorAll,
            t = Document.prototype.querySelectorAll,
            B = DocumentFragment.prototype.querySelectorAll,
            Z = function() {
                if (!c) {
                    var a = document.createElement("template"),
                        b = document.createElement("template");
                    b.content.appendChild(document.createElement("div"));
                    a.content.appendChild(b);
                    a = a.cloneNode(!0);
                    return 0 === a.content.childNodes.length || 0 === a.content.firstChild.content.childNodes.length || d
                }
            }();
        if (c) {
            var P = document.implementation.createHTMLDocument("template"),
                Ka = !0,
                ba = document.createElement("style");
            ba.textContent = "template{display:none;}";
            var La = document.head;
            La.insertBefore(ba, La.firstElementChild);
            a.prototype = Object.create(HTMLElement.prototype);
            var C = !document.createElement("div").hasOwnProperty("innerHTML");
            a.H = function(b) {
                if (!b.content) {
                    b.content = P.createDocumentFragment();
                    for (var c; c = b.firstChild;) l.call(b.content, c);
                    if (C) b.__proto__ = a.prototype;
                    else if (b.cloneNode = function(b) {
                            return a.ga(this, b)
                        }, Ka) try {
                        va(b), ja(b)
                    } catch (qh) {
                        Ka = !1
                    }
                    a.M(b.content)
                }
            };
            var va = function(b) {
                    Object.defineProperty(b, "innerHTML", {
                        get: function() {
                            return nb(this)
                        },
                        set: function(b) {
                            P.body.innerHTML = b;
                            for (a.M(P); this.content.firstChild;) k.call(this.content, this.content.firstChild);
                            for (; P.body.firstChild;) l.call(this.content, P.body.firstChild)
                        },
                        configurable: !0
                    })
                },
                ja = function(a) {
                    Object.defineProperty(a, "outerHTML", {
                        get: function() {
                            return "<template>" + this.innerHTML + "</template>"
                        },
                        set: function(a) {
                            if (this.parentNode) {
                                P.body.innerHTML = a;
                                for (a = this.ownerDocument.createDocumentFragment(); P.body.firstChild;) l.call(a, P.body.firstChild);
                                m.call(this.parentNode, a, this)
                            } else throw Error("Failed to set the 'outerHTML' property on 'Element': This element has no parent node.");
                        },
                        configurable: !0
                    })
                };
            va(a.prototype);
            ja(a.prototype);
            a.M = function(c) {
                c = b(c, "template");
                for (var d = 0, e = c.length, f; d < e && (f = c[d]); d++) a.H(f)
            };
            document.addEventListener("DOMContentLoaded", function() {
                a.M(document)
            });
            Document.prototype.createElement = function() {
                var b = h.apply(this, arguments);
                "template" === b.localName && a.H(b);
                return b
            };
            var wa = /[&\u00A0"]/g,
                ca = /[&\u00A0<>]/g,
                ka = function(a) {
                    switch (a) {
                        case "&":
                            return "&amp;";
                        case "<":
                            return "&lt;";
                        case ">":
                            return "&gt;";
                        case '"':
                            return "&quot;";
                        case "\u00a0":
                            return "&nbsp;"
                    }
                };
            ba = function(a) {
                for (var b = {}, c = 0; c < a.length; c++) b[a[c]] = !0;
                return b
            };
            var gf = ba("area base br col command embed hr img input keygen link meta param source track wbr".split(" ")),
                xa = ba("style script xmp iframe noembed noframes plaintext noscript".split(" ")),
                nb = function(a, b) {
                    "template" === a.localName && (a = a.content);
                    for (var c = "", d = b ? b(a) : a.childNodes, e = 0, f = d.length, h; e < f && (h = d[e]); e++) {
                        a: {
                            var g = h;
                            var k = a;
                            var l = b;
                            switch (g.nodeType) {
                                case Node.ELEMENT_NODE:
                                    for (var m = g.localName, n = "<" + m, ya = g.attributes, Ma =
                                            0; k = ya[Ma]; Ma++) n += " " + k.name + '="' + k.value.replace(wa, ka) + '"';
                                    n += ">";
                                    g = gf[m] ? n : n + nb(g, l) + "</" + m + ">";
                                    break a;
                                case Node.TEXT_NODE:
                                    g = g.data;
                                    g = k && xa[k.localName] ? g : g.replace(ca, ka);
                                    break a;
                                case Node.COMMENT_NODE:
                                    g = "\x3c!--" + g.data + "--\x3e";
                                    break a;
                                default:
                                    throw window.console.error(g), Error("not implemented");
                            }
                        }
                        c += g
                    }
                    return c
                }
        }
        if (c || Z) {
            a.ga = function(a, b) {
                var c = f.call(a, !1);
                this.H && this.H(c);
                b && (l.call(c.content, f.call(a.content, !0)), ya(c.content, a.content));
                return c
            };
            var ya = function(c, d) {
                    if (d.querySelectorAll &&
                        (d = b(d, "template"), 0 !== d.length)) {
                        c = b(c, "template");
                        for (var e = 0, f = c.length, h, g; e < f; e++) g = d[e], h = c[e], a && a.H && a.H(g), m.call(h.parentNode, Ma.call(g, !0), h)
                    }
                },
                Ma = Node.prototype.cloneNode = function(b) {
                    if (!e && d && this instanceof DocumentFragment)
                        if (b) var c = hf.call(this.ownerDocument, this, !0);
                        else return this.ownerDocument.createDocumentFragment();
                    else c = this.nodeType === Node.ELEMENT_NODE && "template" === this.localName ? a.ga(this, b) : f.call(this, b);
                    b && ya(c, this);
                    return c
                },
                hf = Document.prototype.importNode = function(c,
                    d) {
                    d = d || !1;
                    if ("template" === c.localName) return a.ga(c, d);
                    var e = g.call(this, c, d);
                    if (d) {
                        ya(e, c);
                        c = b(e, 'script:not([type]),script[type="application/javascript"],script[type="text/javascript"]');
                        for (var f, k = 0; k < c.length; k++) {
                            f = c[k];
                            d = h.call(document, "script");
                            d.textContent = f.textContent;
                            for (var l = f.attributes, ka = 0, xa; ka < l.length; ka++) xa = l[ka], d.setAttribute(xa.name, xa.value);
                            m.call(f.parentNode, d, f)
                        }
                    }
                    return e
                }
        }
        c && (window.HTMLTemplateElement = a)
    })();
    var na = Array.isArray ? Array.isArray : function(a) {
        return "[object Array]" === Object.prototype.toString.call(a)
    };
    var oa = 0,
        pa, qa = "undefined" !== typeof window ? window : void 0,
        ra = qa || {},
        sa = ra.MutationObserver || ra.WebKitMutationObserver,
        ta = "undefined" !== typeof Uint8ClampedArray && "undefined" !== typeof importScripts && "undefined" !== typeof MessageChannel;

    function ua() {
        return "undefined" !== typeof pa ? function() {
            pa(za)
        } : Aa()
    }

    function Ba() {
        var a = 0,
            b = new sa(za),
            c = document.createTextNode("");
        b.observe(c, {
            characterData: !0
        });
        return function() {
            c.data = a = ++a % 2
        }
    }

    function Ca() {
        var a = new MessageChannel;
        a.port1.onmessage = za;
        return function() {
            return a.port2.postMessage(0)
        }
    }

    function Aa() {
        var a = setTimeout;
        return function() {
            return a(za, 1)
        }
    }
    var Da = Array(1E3);

    function za() {
        for (var a = 0; a < oa; a += 2)(0, Da[a])(Da[a + 1]), Da[a] = void 0, Da[a + 1] = void 0;
        oa = 0
    }
    var Ea, Fa;
    if ("undefined" === typeof self && "undefined" !== typeof process && "[object process]" === {}.toString.call(process)) Fa = function() {
        return process.sb(za)
    };
    else {
        var Ga;
        if (sa) Ga = Ba();
        else {
            var Ha;
            if (ta) Ha = Ca();
            else {
                var Ia;
                if (void 0 === qa && "function" === typeof require) try {
                    var Ja = require("vertx");
                    pa = Ja.ub || Ja.tb;
                    Ia = ua()
                } catch (a) {
                    Ia = Aa()
                } else Ia = Aa();
                Ha = Ia
            }
            Ga = Ha
        }
        Fa = Ga
    }
    Ea = Fa;

    function Na(a, b) {
        Da[oa] = a;
        Da[oa + 1] = b;
        oa += 2;
        2 === oa && Ea()
    };

    function Oa(a, b) {
        var c = this,
            d = new this.constructor(Pa);
        void 0 === d[Qa] && Ra(d);
        var e = c.h;
        if (e) {
            var f = arguments[e - 1];
            Na(function() {
                return Sa(e, d, f, c.f)
            })
        } else Ta(c, d, a, b);
        return d
    };

    function Ua(a) {
        if (a && "object" === typeof a && a.constructor === this) return a;
        var b = new this(Pa);
        Va(b, a);
        return b
    };
    var Qa = Math.random().toString(36).substring(16);

    function Pa() {}
    var Xa = new Wa;

    function Ya(a) {
        try {
            return a.then
        } catch (b) {
            return Xa.error = b, Xa
        }
    }

    function Za(a, b, c, d) {
        try {
            a.call(b, c, d)
        } catch (e) {
            return e
        }
    }

    function $a(a, b, c) {
        Na(function(a) {
            var d = !1,
                f = Za(c, b, function(c) {
                    d || (d = !0, b !== c ? Va(a, c) : r(a, c))
                }, function(b) {
                    d || (d = !0, u(a, b))
                });
            !d && f && (d = !0, u(a, f))
        }, a)
    }

    function ab(a, b) {
        1 === b.h ? r(a, b.f) : 2 === b.h ? u(a, b.f) : Ta(b, void 0, function(b) {
            return Va(a, b)
        }, function(b) {
            return u(a, b)
        })
    }

    function bb(a, b, c) {
        b.constructor === a.constructor && c === Oa && b.constructor.resolve === Ua ? ab(a, b) : c === Xa ? (u(a, Xa.error), Xa.error = null) : void 0 === c ? r(a, b) : "function" === typeof c ? $a(a, b, c) : r(a, b)
    }

    function Va(a, b) {
        if (a === b) u(a, new TypeError("You cannot resolve a promise with itself"));
        else {
            var c = typeof b;
            null === b || "object" !== c && "function" !== c ? r(a, b) : bb(a, b, Ya(b))
        }
    }

    function cb(a) {
        a.qa && a.qa(a.f);
        db(a)
    }

    function r(a, b) {
        void 0 === a.h && (a.f = b, a.h = 1, 0 !== a.L.length && Na(db, a))
    }

    function u(a, b) {
        void 0 === a.h && (a.h = 2, a.f = b, Na(cb, a))
    }

    function Ta(a, b, c, d) {
        var e = a.L,
            f = e.length;
        a.qa = null;
        e[f] = b;
        e[f + 1] = c;
        e[f + 2] = d;
        0 === f && a.h && Na(db, a)
    }

    function db(a) {
        var b = a.L,
            c = a.h;
        if (0 !== b.length) {
            for (var d, e, f = a.f, h = 0; h < b.length; h += 3) d = b[h], e = b[h + c], d ? Sa(c, d, e, f) : e(f);
            a.L.length = 0
        }
    }

    function Wa() {
        this.error = null
    }
    var eb = new Wa;

    function Sa(a, b, c, d) {
        var e = "function" === typeof c;
        if (e) {
            try {
                var f = c(d)
            } catch (l) {
                eb.error = l, f = eb
            }
            if (f === eb) {
                var h = !0;
                var g = f.error;
                f.error = null
            } else var k = !0;
            if (b === f) {
                u(b, new TypeError("A promises callback cannot return that same promise."));
                return
            }
        } else f = d, k = !0;
        void 0 === b.h && (e && k ? Va(b, f) : h ? u(b, g) : 1 === a ? r(b, f) : 2 === a && u(b, f))
    }

    function fb(a, b) {
        try {
            b(function(b) {
                Va(a, b)
            }, function(b) {
                u(a, b)
            })
        } catch (c) {
            u(a, c)
        }
    }
    var gb = 0;

    function Ra(a) {
        a[Qa] = gb++;
        a.h = void 0;
        a.f = void 0;
        a.L = []
    };

    function hb(a, b) {
        this.Ha = a;
        this.D = new a(Pa);
        this.D[Qa] || Ra(this.D);
        if (na(b))
            if (this.U = this.length = b.length, this.f = Array(this.length), 0 === this.length) r(this.D, this.f);
            else {
                this.length = this.length || 0;
                for (a = 0; void 0 === this.h && a < b.length; a++) ib(this, b[a], a);
                0 === this.U && r(this.D, this.f)
            } else u(this.D, Error("Array Methods must be provided an Array"))
    }

    function ib(a, b, c) {
        var d = a.Ha,
            e = d.resolve;
        e === Ua ? (e = Ya(b), e === Oa && void 0 !== b.h ? jb(a, b.h, c, b.f) : "function" !== typeof e ? (a.U--, a.f[c] = b) : d === v ? (d = new d(Pa), bb(d, b, e), kb(a, d, c)) : kb(a, new d(function(a) {
            return a(b)
        }), c)) : kb(a, e(b), c)
    }

    function jb(a, b, c, d) {
        var e = a.D;
        void 0 === e.h && (a.U--, 2 === b ? u(e, d) : a.f[c] = d);
        0 === a.U && r(e, a.f)
    }

    function kb(a, b, c) {
        Ta(b, void 0, function(b) {
            return jb(a, 1, c, b)
        }, function(b) {
            return jb(a, 2, c, b)
        })
    };

    function lb(a) {
        return (new hb(this, a)).D
    };

    function mb(a) {
        var b = this;
        return na(a) ? new b(function(c, d) {
            for (var e = a.length, f = 0; f < e; f++) b.resolve(a[f]).then(c, d)
        }) : new b(function(a, b) {
            return b(new TypeError("You must pass an array to race."))
        })
    };

    function ob(a) {
        var b = new this(Pa);
        u(b, a);
        return b
    };

    function v(a) {
        this[Qa] = gb++;
        this.f = this.h = void 0;
        this.L = [];
        if (Pa !== a) {
            if ("function" !== typeof a) throw new TypeError("You must pass a resolver function as the first argument to the promise constructor");
            if (this instanceof v) fb(this, a);
            else throw new TypeError("Failed to construct 'Promise': Please use the 'new' operator, this object constructor cannot be called as a function.");
        }
    }
    v.prototype = {
        constructor: v,
        then: Oa,
        a: function(a) {
            return this.then(null, a)
        }
    };
    /*

    Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
    This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
    The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
    The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
    Code distributed by Google as part of the polymer project is also
    subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
    */
    window.Promise || (window.Promise = v, v.prototype["catch"] = v.prototype.a, v.prototype.then = v.prototype.then, v.all = lb, v.race = mb, v.resolve = Ua, v.reject = ob);
    (function(a) {
        function b(a, b) {
            if ("function" === typeof window.CustomEvent) return new CustomEvent(a, b);
            var c = document.createEvent("CustomEvent");
            c.initCustomEvent(a, !!b.bubbles, !!b.cancelable, b.detail);
            return c
        }

        function c(a) {
            if (B) return a.ownerDocument !== document ? a.ownerDocument : null;
            var b = a.__importDoc;
            if (!b && a.parentNode) {
                b = a.parentNode;
                if ("function" === typeof b.closest) b = b.closest("link[rel=import]");
                else
                    for (; !g(b) && (b = b.parentNode););
                a.__importDoc = b
            }
            return b
        }

        function d(a) {
            var b = m(document, "link[rel=import]:not([import-dependency])"),
                c = b.length;
            c ? n(b, function(b) {
                return h(b, function() {
                    0 === --c && a()
                })
            }) : a()
        }

        function e(a) {
            function b() {
                "loading" !== document.readyState && document.body && (document.removeEventListener("readystatechange", b), a())
            }
            document.addEventListener("readystatechange", b);
            b()
        }

        function f(a) {
            e(function() {
                return d(function() {
                    return a && a()
                })
            })
        }

        function h(a, b) {
            if (a.__loaded) b && b();
            else if ("script" === a.localName && !a.src || "style" === a.localName && !a.firstChild) a.__loaded = !0, b && b();
            else {
                var c = function(d) {
                    a.removeEventListener(d.type,
                        c);
                    a.__loaded = !0;
                    b && b()
                };
                a.addEventListener("load", c);
                ja && "style" === a.localName || a.addEventListener("error", c)
            }
        }

        function g(a) {
            return a.nodeType === Node.ELEMENT_NODE && "link" === a.localName && "import" === a.rel
        }

        function k() {
            var a = this;
            this.a = {};
            this.b = 0;
            this.c = new MutationObserver(function(b) {
                return a.Ua(b)
            });
            this.c.observe(document.head, {
                childList: !0,
                subtree: !0
            });
            this.loadImports(document)
        }

        function l(a) {
            n(m(a, "template"), function(a) {
                n(m(a.content, 'script:not([type]),script[type="application/javascript"],script[type="text/javascript"]'),
                    function(a) {
                        var b = document.createElement("script");
                        n(a.attributes, function(a) {
                            return b.setAttribute(a.name, a.value)
                        });
                        b.textContent = a.textContent;
                        a.parentNode.replaceChild(b, a)
                    });
                l(a.content)
            })
        }

        function m(a, b) {
            return a.childNodes.length ? a.querySelectorAll(b) : Z
        }

        function n(a, b, c) {
            var d = a ? a.length : 0,
                e = c ? -1 : 1;
            for (c = c ? d - 1 : 0; c < d && 0 <= c; c += e) b(a[c], c)
        }
        var t = document.createElement("link"),
            B = "import" in t,
            Z = t.querySelectorAll("*"),
            P = null;
        !1 === "currentScript" in document && Object.defineProperty(document, "currentScript", {
            get: function() {
                return P || ("complete" !== document.readyState ? document.scripts[document.scripts.length - 1] : null)
            },
            configurable: !0
        });
        var Ka = /(url\()([^)]*)(\))/g,
            ba = /(@import[\s]+(?!url\())([^;]*)(;)/g,
            La = /(<link[^>]*)(rel=['|"]?stylesheet['|"]?[^>]*>)/g,
            C = {
                Oa: function(a, b) {
                    a.href && a.setAttribute("href", C.aa(a.getAttribute("href"), b));
                    a.src && a.setAttribute("src", C.aa(a.getAttribute("src"), b));
                    if ("style" === a.localName) {
                        var c = C.xa(a.textContent, b, Ka);
                        a.textContent = C.xa(c, b, ba)
                    }
                },
                xa: function(a, b, c) {
                    return a.replace(c,
                        function(a, c, d, e) {
                            a = d.replace(/["']/g, "");
                            b && (a = C.aa(a, b));
                            return c + "'" + a + "'" + e
                        })
                },
                aa: function(a, b) {
                    if (void 0 === C.fa) {
                        C.fa = !1;
                        try {
                            var c = new URL("b", "http://a");
                            c.pathname = "c%20d";
                            C.fa = "http://a/c%20d" === c.href
                        } catch (nb) {}
                    }
                    if (C.fa) return (new URL(a, b)).href;
                    c = C.Ea;
                    c || (c = document.implementation.createHTMLDocument("temp"), C.Ea = c, c.oa = c.createElement("base"), c.head.appendChild(c.oa), c.na = c.createElement("a"));
                    c.oa.href = b;
                    c.na.href = a;
                    return c.na.href || a
                }
            },
            va = {
                async: !0,
                load: function(a, b, c) {
                    if (a)
                        if (a.match(/^data:/)) {
                            a =
                                a.split(",");
                            var d = a[1];
                            d = -1 < a[0].indexOf(";base64") ? atob(d) : decodeURIComponent(d);
                            b(d)
                        } else {
                            var e = new XMLHttpRequest;
                            e.open("GET", a, va.async);
                            e.onload = function() {
                                var a = e.responseURL || e.getResponseHeader("Location");
                                a && 0 === a.indexOf("/") && (a = (location.origin || location.protocol + "//" + location.host) + a);
                                var d = e.response || e.responseText;
                                304 === e.status || 0 === e.status || 200 <= e.status && 300 > e.status ? b(d, a) : c(d)
                            };
                            e.send()
                        } else c("error: href must be specified")
                }
            },
            ja = /Trident/.test(navigator.userAgent) || /Edge\/\d./i.test(navigator.userAgent);
        k.prototype.loadImports = function(a) {
            var b = this;
            a = m(a, "link[rel=import]");
            n(a, function(a) {
                return b.s(a)
            })
        };
        k.prototype.s = function(a) {
            var b = this,
                c = a.href;
            if (void 0 !== this.a[c]) {
                var d = this.a[c];
                d && d.__loaded && (a.__import = d, this.i(a))
            } else this.b++, this.a[c] = "pending", va.load(c, function(a, d) {
                a = b.Va(a, d || c);
                b.a[c] = a;
                b.b--;
                b.loadImports(a);
                b.N()
            }, function() {
                b.a[c] = null;
                b.b--;
                b.N()
            })
        };
        k.prototype.Va = function(a, b) {
            if (!a) return document.createDocumentFragment();
            ja && (a = a.replace(La, function(a, b, c) {
                return -1 ===
                    a.indexOf("type=") ? b + " type=import-disable " + c : a
            }));
            var c = document.createElement("template");
            c.innerHTML = a;
            if (c.content) a = c.content, l(a);
            else
                for (a = document.createDocumentFragment(); c.firstChild;) a.appendChild(c.firstChild);
            if (c = a.querySelector("base")) b = C.aa(c.getAttribute("href"), b), c.removeAttribute("href");
            c = m(a, 'link[rel=import],link[rel=stylesheet][href][type=import-disable],style:not([type]),link[rel=stylesheet][href]:not([type]),script:not([type]),script[type="application/javascript"],script[type="text/javascript"]');
            var d = 0;
            n(c, function(a) {
                h(a);
                C.Oa(a, b);
                a.setAttribute("import-dependency", "");
                "script" === a.localName && !a.src && a.textContent && (a.setAttribute("src", "data:text/javascript;charset=utf-8," + encodeURIComponent(a.textContent + ("\n//# sourceURL=" + b + (d ? "-" + d : "") + ".js\n"))), a.textContent = "", d++)
            });
            return a
        };
        k.prototype.N = function() {
            var a = this;
            if (!this.b) {
                this.c.disconnect();
                this.flatten(document);
                var b = !1,
                    c = !1,
                    d = function() {
                        c && b && (a.loadImports(document), a.b || (a.c.observe(document.head, {
                                childList: !0,
                                subtree: !0
                            }),
                            a.Sa()))
                    };
                this.Xa(function() {
                    c = !0;
                    d()
                });
                this.Wa(function() {
                    b = !0;
                    d()
                })
            }
        };
        k.prototype.flatten = function(a) {
            var b = this;
            a = m(a, "link[rel=import]");
            n(a, function(a) {
                var c = b.a[a.href];
                (a.__import = c) && c.nodeType === Node.DOCUMENT_FRAGMENT_NODE && (b.a[a.href] = a, a.readyState = "loading", a.__import = a, b.flatten(c), a.appendChild(c))
            })
        };
        k.prototype.Wa = function(a) {
            function b(e) {
                if (e < d) {
                    var f = c[e],
                        g = document.createElement("script");
                    f.removeAttribute("import-dependency");
                    n(f.attributes, function(a) {
                        return g.setAttribute(a.name,
                            a.value)
                    });
                    P = g;
                    f.parentNode.replaceChild(g, f);
                    h(g, function() {
                        P = null;
                        b(e + 1)
                    })
                } else a()
            }
            var c = m(document, "script[import-dependency]"),
                d = c.length;
            b(0)
        };
        k.prototype.Xa = function(a) {
            var b = m(document, "style[import-dependency],link[rel=stylesheet][import-dependency]"),
                d = b.length;
            if (d) {
                var e = ja && !!document.querySelector("link[rel=stylesheet][href][type=import-disable]");
                n(b, function(b) {
                    h(b, function() {
                        b.removeAttribute("import-dependency");
                        0 === --d && a()
                    });
                    if (e && b.parentNode !== document.head) {
                        var f = document.createElement(b.localName);
                        f.__appliedElement = b;
                        f.setAttribute("type", "import-placeholder");
                        b.parentNode.insertBefore(f, b.nextSibling);
                        for (f = c(b); f && c(f);) f = c(f);
                        f.parentNode !== document.head && (f = null);
                        document.head.insertBefore(b, f);
                        b.removeAttribute("type")
                    }
                })
            } else a()
        };
        k.prototype.Sa = function() {
            var a = this,
                b = m(document, "link[rel=import]");
            n(b, function(b) {
                return a.i(b)
            }, !0)
        };
        k.prototype.i = function(a) {
            a.__loaded || (a.__loaded = !0, a.import && (a.import.readyState = "complete"), a.dispatchEvent(b(a.import ? "load" : "error", {
                bubbles: !1,
                cancelable: !1,
                detail: void 0
            })))
        };
        k.prototype.Ua = function(a) {
            var b = this;
            n(a, function(a) {
                return n(a.addedNodes, function(a) {
                    a && a.nodeType === Node.ELEMENT_NODE && (g(a) ? b.s(a) : b.loadImports(a))
                })
            })
        };
        var wa = null;
        if (B) t = m(document, "link[rel=import]"), n(t, function(a) {
            a.import && "loading" === a.import.readyState || (a.__loaded = !0)
        }), t = function(a) {
            a = a.target;
            g(a) && (a.__loaded = !0)
        }, document.addEventListener("load", t, !0), document.addEventListener("error", t, !0);
        else {
            var ca = Object.getOwnPropertyDescriptor(Node.prototype,
                "baseURI");
            Object.defineProperty((!ca || ca.configurable ? Node : Element).prototype, "baseURI", {
                get: function() {
                    var a = g(this) ? this : c(this);
                    return a ? a.href : ca && ca.get ? ca.get.call(this) : (document.querySelector("base") || window.location).href
                },
                configurable: !0,
                enumerable: !0
            });
            Object.defineProperty(HTMLLinkElement.prototype, "import", {
                get: function() {
                    return this.__import || null
                },
                configurable: !0,
                enumerable: !0
            });
            e(function() {
                wa = new k
            })
        }
        f(function() {
            return document.dispatchEvent(b("HTMLImportsLoaded", {
                cancelable: !0,
                bubbles: !0,
                detail: void 0
            }))
        });
        a.useNative = B;
        a.whenReady = f;
        a.importForElement = c;
        a.loadImports = function(a) {
            wa && wa.loadImports(a)
        }
    })(window.HTMLImports = window.HTMLImports || {});
    /*

     Copyright (c) 2014 The Polymer Project Authors. All rights reserved.
     This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
     The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
     The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
     Code distributed by Google as part of the polymer project is also
     subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
    */
    window.WebComponents = window.WebComponents || {
        flags: {}
    };
    var pb = document.querySelector('script[src*="webcomponents-lite.js"]'),
        qb = /wc-(.+)/,
        w = {};
    if (!w.noOpts) {
        location.search.slice(1).split("&").forEach(function(a) {
            a = a.split("=");
            var b;
            a[0] && (b = a[0].match(qb)) && (w[b[1]] = a[1] || !0)
        });
        if (pb)
            for (var rb = 0, sb; sb = pb.attributes[rb]; rb++) "src" !== sb.name && (w[sb.name] = sb.value || !0);
        if (w.log && w.log.split) {
            var tb = w.log.split(",");
            w.log = {};
            tb.forEach(function(a) {
                w.log[a] = !0
            })
        } else w.log = {}
    }
    window.WebComponents.flags = w;
    var ub = w.shadydom;
    ub && (window.ShadyDOM = window.ShadyDOM || {}, window.ShadyDOM.force = ub);
    var vb = w.register || w.ce;
    vb && window.customElements && (window.customElements.forcePolyfill = vb);
    /*

    Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
    This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
    The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
    The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
    Code distributed by Google as part of the polymer project is also
    subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
    */
    function wb() {
        this.wa = this.root = null;
        this.Y = !1;
        this.C = this.T = this.ka = this.assignedSlot = this.assignedNodes = this.J = null;
        this.childNodes = this.nextSibling = this.previousSibling = this.lastChild = this.firstChild = this.parentNode = this.O = void 0;
        this.Ca = this.pa = !1
    }

    function x(a) {
        a.da || (a.da = new wb);
        return a.da
    }

    function y(a) {
        return a && a.da
    };
    var z = window.ShadyDOM || {};
    z.Qa = !(!Element.prototype.attachShadow || !Node.prototype.getRootNode);
    var xb = Object.getOwnPropertyDescriptor(Node.prototype, "firstChild");
    z.w = !!(xb && xb.configurable && xb.get);
    z.ua = z.force || !z.Qa;
    var yb = navigator.userAgent.match("Trident"),
        zb = navigator.userAgent.match("Edge");
    void 0 === z.Aa && (z.Aa = z.w && (yb || zb));

    function Ab(a) {
        return (a = y(a)) && void 0 !== a.firstChild
    }

    function A(a) {
        return "ShadyRoot" === a.Ia
    }

    function Bb(a) {
        a = a.getRootNode();
        if (A(a)) return a
    }
    var Cb = Element.prototype,
        Db = Cb.matches || Cb.matchesSelector || Cb.mozMatchesSelector || Cb.msMatchesSelector || Cb.oMatchesSelector || Cb.webkitMatchesSelector;

    function Eb(a, b) {
        if (a && b)
            for (var c = Object.getOwnPropertyNames(b), d = 0, e; d < c.length && (e = c[d]); d++) {
                var f = Object.getOwnPropertyDescriptor(b, e);
                f && Object.defineProperty(a, e, f)
            }
    }

    function Fb(a, b) {
        for (var c = [], d = 1; d < arguments.length; ++d) c[d - 1] = arguments[d];
        for (d = 0; d < c.length; d++) Eb(a, c[d]);
        return a
    }

    function Gb(a, b) {
        for (var c in b) a[c] = b[c]
    }
    var Hb = document.createTextNode(""),
        Ib = 0,
        Jb = [];
    (new MutationObserver(function() {
        for (; Jb.length;) try {
            Jb.shift()()
        } catch (a) {
            throw Hb.textContent = Ib++, a;
        }
    })).observe(Hb, {
        characterData: !0
    });

    function Kb(a) {
        Jb.push(a);
        Hb.textContent = Ib++
    }
    var Lb = !!document.contains;

    function Mb(a, b) {
        for (; b;) {
            if (b == a) return !0;
            b = b.parentNode
        }
        return !1
    };
    var Nb = [],
        Ob;

    function Pb(a) {
        Ob || (Ob = !0, Kb(Qb));
        Nb.push(a)
    }

    function Qb() {
        Ob = !1;
        for (var a = !!Nb.length; Nb.length;) Nb.shift()();
        return a
    }
    Qb.list = Nb;

    function Rb() {
        this.a = !1;
        this.addedNodes = [];
        this.removedNodes = [];
        this.X = new Set
    }

    function Sb(a) {
        a.a || (a.a = !0, Kb(function() {
            Tb(a)
        }))
    }

    function Tb(a) {
        if (a.a) {
            a.a = !1;
            var b = a.takeRecords();
            b.length && a.X.forEach(function(a) {
                a(b)
            })
        }
    }
    Rb.prototype.takeRecords = function() {
        if (this.addedNodes.length || this.removedNodes.length) {
            var a = [{
                addedNodes: this.addedNodes,
                removedNodes: this.removedNodes
            }];
            this.addedNodes = [];
            this.removedNodes = [];
            return a
        }
        return []
    };

    function Ub(a, b) {
        var c = x(a);
        c.J || (c.J = new Rb);
        c.J.X.add(b);
        var d = c.J;
        return {
            Fa: b,
            G: d,
            Ja: a,
            takeRecords: function() {
                return d.takeRecords()
            }
        }
    }

    function Vb(a) {
        var b = a && a.G;
        b && (b.X.delete(a.Fa), b.X.size || (x(a.Ja).J = null))
    }

    function Wb(a, b) {
        var c = b.getRootNode();
        return a.map(function(a) {
            var b = c === a.target.getRootNode();
            if (b && a.addedNodes) {
                if (b = Array.from(a.addedNodes).filter(function(a) {
                        return c === a.getRootNode()
                    }), b.length) return a = Object.create(a), Object.defineProperty(a, "addedNodes", {
                    value: b,
                    configurable: !0
                }), a
            } else if (b) return a
        }).filter(function(a) {
            return a
        })
    };
    var D = {},
        Xb = Element.prototype.insertBefore,
        Yb = Element.prototype.replaceChild,
        Zb = Element.prototype.removeChild,
        $b = Element.prototype.setAttribute,
        ac = Element.prototype.removeAttribute,
        bc = Element.prototype.cloneNode,
        cc = Document.prototype.importNode,
        dc = Element.prototype.addEventListener,
        ec = Element.prototype.removeEventListener,
        fc = Window.prototype.addEventListener,
        gc = Window.prototype.removeEventListener,
        hc = Element.prototype.dispatchEvent,
        ic = Node.prototype.contains || HTMLElement.prototype.contains,
        jc = Element.prototype.querySelector,
        kc = DocumentFragment.prototype.querySelector,
        lc = Document.prototype.querySelector,
        mc = Element.prototype.querySelectorAll,
        nc = DocumentFragment.prototype.querySelectorAll,
        oc = Document.prototype.querySelectorAll;
    D.appendChild = Element.prototype.appendChild;
    D.insertBefore = Xb;
    D.replaceChild = Yb;
    D.removeChild = Zb;
    D.setAttribute = $b;
    D.removeAttribute = ac;
    D.cloneNode = bc;
    D.importNode = cc;
    D.addEventListener = dc;
    D.removeEventListener = ec;
    D.cb = fc;
    D.eb = gc;
    D.dispatchEvent = hc;
    D.contains = ic;
    D.mb = jc;
    D.pb = kc;
    D.kb = lc;
    D.querySelector = function(a) {
        switch (this.nodeType) {
            case Node.ELEMENT_NODE:
                return jc.call(this, a);
            case Node.DOCUMENT_NODE:
                return lc.call(this, a);
            default:
                return kc.call(this, a)
        }
    };
    D.nb = mc;
    D.qb = nc;
    D.lb = oc;
    D.querySelectorAll = function(a) {
        switch (this.nodeType) {
            case Node.ELEMENT_NODE:
                return mc.call(this, a);
            case Node.DOCUMENT_NODE:
                return oc.call(this, a);
            default:
                return nc.call(this, a)
        }
    };
    var pc = /[&\u00A0"]/g,
        qc = /[&\u00A0<>]/g;

    function rc(a) {
        switch (a) {
            case "&":
                return "&amp;";
            case "<":
                return "&lt;";
            case ">":
                return "&gt;";
            case '"':
                return "&quot;";
            case "\u00a0":
                return "&nbsp;"
        }
    }

    function sc(a) {
        for (var b = {}, c = 0; c < a.length; c++) b[a[c]] = !0;
        return b
    }
    var tc = sc("area base br col command embed hr img input keygen link meta param source track wbr".split(" ")),
        uc = sc("style script xmp iframe noembed noframes plaintext noscript".split(" "));

    function vc(a, b) {
        "template" === a.localName && (a = a.content);
        for (var c = "", d = b ? b(a) : a.childNodes, e = 0, f = d.length, h; e < f && (h = d[e]); e++) {
            a: {
                var g = h;
                var k = a;
                var l = b;
                switch (g.nodeType) {
                    case Node.ELEMENT_NODE:
                        for (var m = g.localName, n = "<" + m, t = g.attributes, B = 0; k = t[B]; B++) n += " " + k.name + '="' + k.value.replace(pc, rc) + '"';
                        n += ">";
                        g = tc[m] ? n : n + vc(g, l) + "</" + m + ">";
                        break a;
                    case Node.TEXT_NODE:
                        g = g.data;
                        g = k && uc[k.localName] ? g : g.replace(qc, rc);
                        break a;
                    case Node.COMMENT_NODE:
                        g = "\x3c!--" + g.data + "--\x3e";
                        break a;
                    default:
                        throw window.console.error(g),
                            Error("not implemented");
                }
            }
            c += g
        }
        return c
    };
    var E = {},
        F = document.createTreeWalker(document, NodeFilter.SHOW_ALL, null, !1),
        G = document.createTreeWalker(document, NodeFilter.SHOW_ELEMENT, null, !1);

    function wc(a) {
        var b = [];
        F.currentNode = a;
        for (a = F.firstChild(); a;) b.push(a), a = F.nextSibling();
        return b
    }
    E.parentNode = function(a) {
        F.currentNode = a;
        return F.parentNode()
    };
    E.firstChild = function(a) {
        F.currentNode = a;
        return F.firstChild()
    };
    E.lastChild = function(a) {
        F.currentNode = a;
        return F.lastChild()
    };
    E.previousSibling = function(a) {
        F.currentNode = a;
        return F.previousSibling()
    };
    E.nextSibling = function(a) {
        F.currentNode = a;
        return F.nextSibling()
    };
    E.childNodes = wc;
    E.parentElement = function(a) {
        G.currentNode = a;
        return G.parentNode()
    };
    E.firstElementChild = function(a) {
        G.currentNode = a;
        return G.firstChild()
    };
    E.lastElementChild = function(a) {
        G.currentNode = a;
        return G.lastChild()
    };
    E.previousElementSibling = function(a) {
        G.currentNode = a;
        return G.previousSibling()
    };
    E.nextElementSibling = function(a) {
        G.currentNode = a;
        return G.nextSibling()
    };
    E.children = function(a) {
        var b = [];
        G.currentNode = a;
        for (a = G.firstChild(); a;) b.push(a), a = G.nextSibling();
        return b
    };
    E.innerHTML = function(a) {
        return vc(a, function(a) {
            return wc(a)
        })
    };
    E.textContent = function(a) {
        switch (a.nodeType) {
            case Node.ELEMENT_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
                a = document.createTreeWalker(a, NodeFilter.SHOW_TEXT, null, !1);
                for (var b = "", c; c = a.nextNode();) b += c.nodeValue;
                return b;
            default:
                return a.nodeValue
        }
    };
    var H = {},
        xc = z.w,
        yc = [Node.prototype, Element.prototype, HTMLElement.prototype];

    function I(a) {
        var b;
        a: {
            for (b = 0; b < yc.length; b++) {
                var c = yc[b];
                if (c.hasOwnProperty(a)) {
                    b = c;
                    break a
                }
            }
            b = void 0
        }
        if (!b) throw Error("Could not find descriptor for " + a);
        return Object.getOwnPropertyDescriptor(b, a)
    }
    var J = xc ? {
            parentNode: I("parentNode"),
            firstChild: I("firstChild"),
            lastChild: I("lastChild"),
            previousSibling: I("previousSibling"),
            nextSibling: I("nextSibling"),
            childNodes: I("childNodes"),
            parentElement: I("parentElement"),
            previousElementSibling: I("previousElementSibling"),
            nextElementSibling: I("nextElementSibling"),
            innerHTML: I("innerHTML"),
            textContent: I("textContent"),
            firstElementChild: I("firstElementChild"),
            lastElementChild: I("lastElementChild"),
            children: I("children")
        } : {},
        zc = xc ? {
            firstElementChild: Object.getOwnPropertyDescriptor(DocumentFragment.prototype,
                "firstElementChild"),
            lastElementChild: Object.getOwnPropertyDescriptor(DocumentFragment.prototype, "lastElementChild"),
            children: Object.getOwnPropertyDescriptor(DocumentFragment.prototype, "children")
        } : {},
        Ac = xc ? {
            firstElementChild: Object.getOwnPropertyDescriptor(Document.prototype, "firstElementChild"),
            lastElementChild: Object.getOwnPropertyDescriptor(Document.prototype, "lastElementChild"),
            children: Object.getOwnPropertyDescriptor(Document.prototype, "children")
        } : {};
    H.va = J;
    H.ob = zc;
    H.jb = Ac;
    H.parentNode = function(a) {
        return J.parentNode.get.call(a)
    };
    H.firstChild = function(a) {
        return J.firstChild.get.call(a)
    };
    H.lastChild = function(a) {
        return J.lastChild.get.call(a)
    };
    H.previousSibling = function(a) {
        return J.previousSibling.get.call(a)
    };
    H.nextSibling = function(a) {
        return J.nextSibling.get.call(a)
    };
    H.childNodes = function(a) {
        return Array.prototype.slice.call(J.childNodes.get.call(a))
    };
    H.parentElement = function(a) {
        return J.parentElement.get.call(a)
    };
    H.previousElementSibling = function(a) {
        return J.previousElementSibling.get.call(a)
    };
    H.nextElementSibling = function(a) {
        return J.nextElementSibling.get.call(a)
    };
    H.innerHTML = function(a) {
        return J.innerHTML.get.call(a)
    };
    H.textContent = function(a) {
        return J.textContent.get.call(a)
    };
    H.children = function(a) {
        switch (a.nodeType) {
            case Node.DOCUMENT_FRAGMENT_NODE:
                a = zc.children.get.call(a);
                break;
            case Node.DOCUMENT_NODE:
                a = Ac.children.get.call(a);
                break;
            default:
                a = J.children.get.call(a)
        }
        return Array.prototype.slice.call(a)
    };
    H.firstElementChild = function(a) {
        switch (a.nodeType) {
            case Node.DOCUMENT_FRAGMENT_NODE:
                return zc.firstElementChild.get.call(a);
            case Node.DOCUMENT_NODE:
                return Ac.firstElementChild.get.call(a);
            default:
                return J.firstElementChild.get.call(a)
        }
    };
    H.lastElementChild = function(a) {
        switch (a.nodeType) {
            case Node.DOCUMENT_FRAGMENT_NODE:
                return zc.lastElementChild.get.call(a);
            case Node.DOCUMENT_NODE:
                return Ac.lastElementChild.get.call(a);
            default:
                return J.lastElementChild.get.call(a)
        }
    };
    var K = z.Aa ? H : E;

    function Bc(a) {
        for (; a.firstChild;) a.removeChild(a.firstChild)
    }
    var Cc = z.w,
        Dc = document.implementation.createHTMLDocument("inert"),
        Ec = Object.getOwnPropertyDescriptor(Node.prototype, "isConnected"),
        Fc = Ec && Ec.get,
        Gc = Object.getOwnPropertyDescriptor(Document.prototype, "activeElement"),
        Hc = {
            parentElement: {
                get: function() {
                    var a = y(this);
                    (a = a && a.parentNode) && a.nodeType !== Node.ELEMENT_NODE && (a = null);
                    return void 0 !== a ? a : K.parentElement(this)
                },
                configurable: !0
            },
            parentNode: {
                get: function() {
                    var a = y(this);
                    a = a && a.parentNode;
                    return void 0 !== a ? a : K.parentNode(this)
                },
                configurable: !0
            },
            nextSibling: {
                get: function() {
                    var a = y(this);
                    a = a && a.nextSibling;
                    return void 0 !== a ? a : K.nextSibling(this)
                },
                configurable: !0
            },
            previousSibling: {
                get: function() {
                    var a = y(this);
                    a = a && a.previousSibling;
                    return void 0 !== a ? a : K.previousSibling(this)
                },
                configurable: !0
            },
            nextElementSibling: {
                get: function() {
                    var a = y(this);
                    if (a && void 0 !== a.nextSibling) {
                        for (a = this.nextSibling; a && a.nodeType !== Node.ELEMENT_NODE;) a = a.nextSibling;
                        return a
                    }
                    return K.nextElementSibling(this)
                },
                configurable: !0
            },
            previousElementSibling: {
                get: function() {
                    var a =
                        y(this);
                    if (a && void 0 !== a.previousSibling) {
                        for (a = this.previousSibling; a && a.nodeType !== Node.ELEMENT_NODE;) a = a.previousSibling;
                        return a
                    }
                    return K.previousElementSibling(this)
                },
                configurable: !0
            }
        },
        Ic = {
            className: {
                get: function() {
                    return this.getAttribute("class") || ""
                },
                set: function(a) {
                    this.setAttribute("class", a)
                },
                configurable: !0
            }
        },
        Jc = {
            childNodes: {
                get: function() {
                    if (Ab(this)) {
                        var a = y(this);
                        if (!a.childNodes) {
                            a.childNodes = [];
                            for (var b = this.firstChild; b; b = b.nextSibling) a.childNodes.push(b)
                        }
                        var c = a.childNodes
                    } else c =
                        K.childNodes(this);
                    c.item = function(a) {
                        return c[a]
                    };
                    return c
                },
                configurable: !0
            },
            childElementCount: {
                get: function() {
                    return this.children.length
                },
                configurable: !0
            },
            firstChild: {
                get: function() {
                    var a = y(this);
                    a = a && a.firstChild;
                    return void 0 !== a ? a : K.firstChild(this)
                },
                configurable: !0
            },
            lastChild: {
                get: function() {
                    var a = y(this);
                    a = a && a.lastChild;
                    return void 0 !== a ? a : K.lastChild(this)
                },
                configurable: !0
            },
            textContent: {
                get: function() {
                    if (Ab(this)) {
                        for (var a = [], b = 0, c = this.childNodes, d; d = c[b]; b++) d.nodeType !== Node.COMMENT_NODE &&
                            a.push(d.textContent);
                        return a.join("")
                    }
                    return K.textContent(this)
                },
                set: function(a) {
                    if ("undefined" === typeof a || null === a) a = "";
                    switch (this.nodeType) {
                        case Node.ELEMENT_NODE:
                        case Node.DOCUMENT_FRAGMENT_NODE:
                            if (!Ab(this) && Cc) {
                                var b = this.firstChild;
                                (b != this.lastChild || b && b.nodeType != Node.TEXT_NODE) && Bc(this);
                                H.va.textContent.set.call(this, a)
                            } else Bc(this), (0 < a.length || this.nodeType === Node.ELEMENT_NODE) && this.appendChild(document.createTextNode(a));
                            break;
                        default:
                            this.nodeValue = a
                    }
                },
                configurable: !0
            },
            firstElementChild: {
                get: function() {
                    var a =
                        y(this);
                    if (a && void 0 !== a.firstChild) {
                        for (a = this.firstChild; a && a.nodeType !== Node.ELEMENT_NODE;) a = a.nextSibling;
                        return a
                    }
                    return K.firstElementChild(this)
                },
                configurable: !0
            },
            lastElementChild: {
                get: function() {
                    var a = y(this);
                    if (a && void 0 !== a.lastChild) {
                        for (a = this.lastChild; a && a.nodeType !== Node.ELEMENT_NODE;) a = a.previousSibling;
                        return a
                    }
                    return K.lastElementChild(this)
                },
                configurable: !0
            },
            children: {
                get: function() {
                    var a = Ab(this) ? Array.prototype.filter.call(this.childNodes, function(a) {
                            return a.nodeType === Node.ELEMENT_NODE
                        }) :
                        K.children(this);
                    a.item = function(b) {
                        return a[b]
                    };
                    return a
                },
                configurable: !0
            },
            innerHTML: {
                get: function() {
                    return Ab(this) ? vc("template" === this.localName ? this.content : this) : K.innerHTML(this)
                },
                set: function(a) {
                    var b = "template" === this.localName ? this.content : this;
                    Bc(b);
                    var c = this.localName;
                    c && "template" !== c || (c = "div");
                    c = Dc.createElement(c);
                    for (Cc ? H.va.innerHTML.set.call(c, a) : c.innerHTML = a; c.firstChild;) b.appendChild(c.firstChild)
                },
                configurable: !0
            }
        },
        Kc = {
            shadowRoot: {
                get: function() {
                    var a = y(this);
                    return a && a.wa ||
                        null
                },
                configurable: !0
            }
        },
        Lc = {
            activeElement: {
                get: function() {
                    var a = Gc && Gc.get ? Gc.get.call(document) : z.w ? void 0 : document.activeElement;
                    if (a && a.nodeType) {
                        var b = !!A(this);
                        if (this === document || b && this.host !== a && D.contains.call(this.host, a)) {
                            for (b = Bb(a); b && b !== this;) a = b.host, b = Bb(a);
                            a = this === document ? b ? null : a : b === this ? a : null
                        } else a = null
                    } else a = null;
                    return a
                },
                set: function() {},
                configurable: !0
            }
        };

    function L(a, b, c) {
        for (var d in b) {
            var e = Object.getOwnPropertyDescriptor(a, d);
            e && e.configurable || !e && c ? Object.defineProperty(a, d, b[d]) : c && console.warn("Could not define", d, "on", a)
        }
    }

    function Mc(a) {
        L(a, Hc);
        L(a, Ic);
        L(a, Jc);
        L(a, Lc)
    }

    function Nc() {
        var a = Oc.prototype;
        a.__proto__ = DocumentFragment.prototype;
        L(a, Hc, !0);
        L(a, Jc, !0);
        L(a, Lc, !0);
        Object.defineProperties(a, {
            nodeType: {
                value: Node.DOCUMENT_FRAGMENT_NODE,
                configurable: !0
            },
            nodeName: {
                value: "#document-fragment",
                configurable: !0
            },
            nodeValue: {
                value: null,
                configurable: !0
            }
        });
        ["localName", "namespaceURI", "prefix"].forEach(function(b) {
            Object.defineProperty(a, b, {
                value: void 0,
                configurable: !0
            })
        });
        ["ownerDocument", "baseURI", "isConnected"].forEach(function(b) {
            Object.defineProperty(a, b, {
                get: function() {
                    return this.host[b]
                },
                configurable: !0
            })
        })
    }
    var Pc = z.w ? function() {} : function(a) {
            var b = x(a);
            b.pa || (b.pa = !0, L(a, Hc, !0), L(a, Ic, !0))
        },
        Qc = z.w ? function() {} : function(a) {
            x(a).Ca || (L(a, Jc, !0), L(a, Kc, !0))
        };
    var Rc = K.childNodes;

    function Sc(a, b, c) {
        Pc(a);
        c = c || null;
        var d = x(a),
            e = x(b),
            f = c ? x(c) : null;
        d.previousSibling = c ? f.previousSibling : b.lastChild;
        if (f = y(d.previousSibling)) f.nextSibling = a;
        if (f = y(d.nextSibling = c)) f.previousSibling = a;
        d.parentNode = b;
        c ? c === e.firstChild && (e.firstChild = a) : (e.lastChild = a, e.firstChild || (e.firstChild = a));
        e.childNodes = null
    }

    function Tc(a, b) {
        var c = x(a);
        if (void 0 === c.firstChild)
            for (b = b || Rc(a), c.firstChild = b[0] || null, c.lastChild = b[b.length - 1] || null, Qc(a), c = 0; c < b.length; c++) {
                var d = b[c],
                    e = x(d);
                e.parentNode = a;
                e.nextSibling = b[c + 1] || null;
                e.previousSibling = b[c - 1] || null;
                Pc(d)
            }
    };
    var Uc = K.parentNode;

    function Vc(a, b, c) {
        if (b === a) throw Error("Failed to execute 'appendChild' on 'Node': The new child element contains the parent.");
        if (c) {
            var d = y(c);
            d = d && d.parentNode;
            if (void 0 !== d && d !== a || void 0 === d && Uc(c) !== a) throw Error("Failed to execute 'insertBefore' on 'Node': The node before which the new node is to be inserted is not a child of this node.");
        }
        if (c === b) return b;
        b.parentNode && Wc(b.parentNode, b);
        var e, f;
        if (!b.__noInsertionPoint) {
            if (f = e = Bb(a)) {
                var h;
                "slot" === b.localName ? h = [b] : b.querySelectorAll &&
                    (h = b.querySelectorAll("slot"));
                f = h && h.length ? h : void 0
            }
            f && (h = e, d = f, h.v = h.v || [], h.g = h.g || [], h.j = h.j || {}, h.v.push.apply(h.v, [].concat(d instanceof Array ? d : ma(la(d)))))
        }("slot" === a.localName || f) && (e = e || Bb(a)) && Xc(e);
        if (Ab(a)) {
            e = c;
            Qc(a);
            f = x(a);
            void 0 !== f.firstChild && (f.childNodes = null);
            if (b.nodeType === Node.DOCUMENT_FRAGMENT_NODE) {
                f = b.childNodes;
                for (h = 0; h < f.length; h++) Sc(f[h], a, e);
                e = x(b);
                f = void 0 !== e.firstChild ? null : void 0;
                e.firstChild = e.lastChild = f;
                e.childNodes = f
            } else Sc(b, a, e);
            e = y(a);
            if (Yc(a)) {
                Xc(e.root);
                var g = !0
            } else e.root && (g = !0)
        }
        g || (g = A(a) ? a.host : a, c ? (c = Zc(c), D.insertBefore.call(g, b, c)) : D.appendChild.call(g, b));
        $c(a, b);
        return b
    }

    function Wc(a, b) {
        if (b.parentNode !== a) throw Error("The node to be removed is not a child of this node: " + b);
        var c = Bb(b),
            d = y(a);
        if (Ab(a)) {
            var e = x(b),
                f = x(a);
            b === f.firstChild && (f.firstChild = e.nextSibling);
            b === f.lastChild && (f.lastChild = e.previousSibling);
            var h = e.previousSibling,
                g = e.nextSibling;
            h && (x(h).nextSibling = g);
            g && (x(g).previousSibling = h);
            e.parentNode = e.previousSibling = e.nextSibling = void 0;
            void 0 !== f.childNodes && (f.childNodes = null);
            if (Yc(a)) {
                Xc(d.root);
                var k = !0
            }
        }
        ad(b);
        if (c) {
            (e = a && "slot" === a.localName) &&
            (k = !0);
            if (c.g) {
                bd(c);
                f = c.j;
                for (Z in f)
                    for (h = f[Z], g = 0; g < h.length; g++) {
                        var l = h[g];
                        if (Mb(b, l)) {
                            h.splice(g, 1);
                            var m = c.g.indexOf(l);
                            0 <= m && c.g.splice(m, 1);
                            g--;
                            m = y(l);
                            if (l = m.C)
                                for (var n = 0; n < l.length; n++) {
                                    var t = l[n],
                                        B = cd(t);
                                    B && D.removeChild.call(B, t)
                                }
                            m.C = [];
                            m.assignedNodes = [];
                            m = !0
                        }
                    }
                var Z = m
            } else Z = void 0;
            (Z || e) && Xc(c)
        }
        k || (k = A(a) ? a.host : a, (!d.root && "slot" !== b.localName || k === Uc(b)) && D.removeChild.call(k, b));
        $c(a, null, b);
        return b
    }

    function ad(a) {
        var b = y(a);
        if (b && void 0 !== b.O) {
            b = a.childNodes;
            for (var c = 0, d = b.length, e; c < d && (e = b[c]); c++) ad(e)
        }
        if (a = y(a)) a.O = void 0
    }

    function Zc(a) {
        var b = a;
        a && "slot" === a.localName && (b = (b = (b = y(a)) && b.C) && b.length ? b[0] : Zc(a.nextSibling));
        return b
    }

    function Yc(a) {
        return (a = (a = y(a)) && a.root) && dd(a)
    }

    function ed(a, b) {
        if ("slot" === b) a = a.parentNode, Yc(a) && Xc(y(a).root);
        else if ("slot" === a.localName && "name" === b && (b = Bb(a))) {
            if (b.g) {
                var c = a.Da,
                    d = fd(a);
                if (d !== c) {
                    c = b.j[c];
                    var e = c.indexOf(a);
                    0 <= e && c.splice(e, 1);
                    c = b.j[d] || (b.j[d] = []);
                    c.push(a);
                    1 < c.length && (b.j[d] = gd(c))
                }
            }
            Xc(b)
        }
    }

    function $c(a, b, c) {
        if (a = (a = y(a)) && a.J) b && a.addedNodes.push(b), c && a.removedNodes.push(c), Sb(a)
    }

    function hd(a) {
        if (a && a.nodeType) {
            var b = x(a),
                c = b.O;
            void 0 === c && (A(a) ? (c = a, b.O = c) : (c = (c = a.parentNode) ? hd(c) : a, D.contains.call(document.documentElement, a) && (b.O = c)));
            return c
        }
    }

    function id(a, b, c) {
        var d = [];
        jd(a.childNodes, b, c, d);
        return d
    }

    function jd(a, b, c, d) {
        for (var e = 0, f = a.length, h; e < f && (h = a[e]); e++) {
            var g;
            if (g = h.nodeType === Node.ELEMENT_NODE) {
                g = h;
                var k = b,
                    l = c,
                    m = d,
                    n = k(g);
                n && m.push(g);
                l && l(n) ? g = n : (jd(g.childNodes, k, l, m), g = void 0)
            }
            if (g) break
        }
    }
    var kd = null;

    function ld(a, b, c) {
        kd || (kd = window.ShadyCSS && window.ShadyCSS.ScopingShim);
        kd && "class" === b ? kd.setElementClass(a, c) : (D.setAttribute.call(a, b, c), ed(a, b))
    }

    function md(a, b) {
        if (a.ownerDocument !== document) return D.importNode.call(document, a, b);
        var c = D.importNode.call(document, a, !1);
        if (b) {
            a = a.childNodes;
            b = 0;
            for (var d; b < a.length; b++) d = md(a[b], !0), c.appendChild(d)
        }
        return c
    };
    var nd = "__eventWrappers" + Date.now(),
        od = {
            blur: !0,
            focus: !0,
            focusin: !0,
            focusout: !0,
            click: !0,
            dblclick: !0,
            mousedown: !0,
            mouseenter: !0,
            mouseleave: !0,
            mousemove: !0,
            mouseout: !0,
            mouseover: !0,
            mouseup: !0,
            wheel: !0,
            beforeinput: !0,
            input: !0,
            keydown: !0,
            keyup: !0,
            compositionstart: !0,
            compositionupdate: !0,
            compositionend: !0,
            touchstart: !0,
            touchend: !0,
            touchmove: !0,
            touchcancel: !0,
            pointerover: !0,
            pointerenter: !0,
            pointerdown: !0,
            pointermove: !0,
            pointerup: !0,
            pointercancel: !0,
            pointerout: !0,
            pointerleave: !0,
            gotpointercapture: !0,
            lostpointercapture: !0,
            dragstart: !0,
            drag: !0,
            dragenter: !0,
            dragleave: !0,
            dragover: !0,
            drop: !0,
            dragend: !0,
            DOMActivate: !0,
            DOMFocusIn: !0,
            DOMFocusOut: !0,
            keypress: !0
        };

    function pd(a, b) {
        var c = [],
            d = a;
        for (a = a === window ? window : (a && a.getRootNode()); d;) c.push(d), d = (d && d.assignedSlot) ? (d && d.assignedSlot) : (d && d.nodeType === Node.DOCUMENT_FRAGMENT_NODE) && d && d.host && (b || d !== a) ? d && d.host : d && d.parentNode;
        c[c.length - 1] === document && c.push(window);
        return c
    }

    function qd(a, b) {
        if (!A) return a;
        a = pd(a, !0);
        for (var c = 0, d, e, f, h; c < b.length; c++)
            if (d = b[c], f = d === window ? window : d.getRootNode(), f !== e && (h = a.indexOf(f), e = f), !A(f) || -1 < h) return d
    }
    var rd = {get composed() {
            !1 !== this.isTrusted && void 0 === this.ba && (this.ba = od[this.type]);
            return this.ba || !1
        },
        composedPath: function() {
            this.b || (this.b = pd(this.__target, this.composed));
            return this.b
        },
        get target() {
            return qd(this.currentTarget, this.composedPath())
        },
        get relatedTarget() {
            if (!this.ca) return null;
            this.c || (this.c = pd(this.ca, !0));
            return qd(this.currentTarget, this.c)
        },
        stopPropagation: function() {
            Event.prototype.stopPropagation.call(this);
            this.a = !0
        },
        stopImmediatePropagation: function() {
            Event.prototype.stopImmediatePropagation.call(this);
            this.a = this.i = !0
        }
    };

    function sd(a) {
        function b(b, d) {
            b = new a(b, d);
            b.ba = d && !!d.composed;
            return b
        }
        Gb(b, a);
        b.prototype = a.prototype;
        return b
    }
    var td = {
        focus: !0,
        blur: !0
    };

    function ud(a) {
        return a.__target !== a.target || a.ca !== a.relatedTarget
    }

    function vd(a, b, c) {
        if (c = b.__handlers && b.__handlers[a.type] && b.__handlers[a.type][c])
            for (var d = 0, e;
                (e = c[d]) && (!ud(a) || a.target !== a.relatedTarget) && (e.call(b, a), !a.i); d++);
    }

    function wd(a) {
        var b = a.composedPath();
        Object.defineProperty(a, "currentTarget", {
            get: function() {
                return d
            },
            configurable: !0
        });
        for (var c = b.length - 1; 0 <= c; c--) {
            var d = b[c];
            vd(a, d, "capture");
            if (a.a) return
        }
        Object.defineProperty(a, "eventPhase", {
            get: function() {
                return Event.AT_TARGET
            }
        });
        var e;
        for (c = 0; c < b.length; c++) {
            d = b[c];
            var f = y(d);
            f = f && f.root;
            if (0 === c || f && f === e)
                if (vd(a, d, "bubble"), d !== window && (e = d.getRootNode()), a.a) break
        }
    }

    function xd(a, b, c, d, e, f) {
        for (var h = 0; h < a.length; h++) {
            var g = a[h],
                k = g.type,
                l = g.capture,
                m = g.once,
                n = g.passive;
            if (b === g.node && c === k && d === l && e === m && f === n) return h
        }
        return -1
    }

    function yd(a, b, c) {
        if (b) {
            var d = typeof b;
            if ("function" === d || "object" === d)
                if ("object" !== d || b.handleEvent && "function" === typeof b.handleEvent) {
                    if (c && "object" === typeof c) {
                        var e = !!c.capture;
                        var f = !!c.once;
                        var h = !!c.passive
                    } else e = !!c, h = f = !1;
                    var g = c && c.ea || this,
                        k = b[nd];
                    if (k) {
                        if (-1 < xd(k, g, a, e, f, h)) return
                    } else b[nd] = [];
                    k = function(e) {
                        f && this.removeEventListener(a, b, c);
                        e.__target || zd(e);
                        if (g !== this) {
                            var h = Object.getOwnPropertyDescriptor(e, "currentTarget");
                            Object.defineProperty(e, "currentTarget", {
                                get: function() {
                                    return g
                                },
                                configurable: !0
                            })
                        }
                        if (e.composed || -1 < e.composedPath().indexOf(g))
                            if (ud(e) && e.target === e.relatedTarget) e.eventPhase === Event.BUBBLING_PHASE && e.stopImmediatePropagation();
                            else if (e.eventPhase === Event.CAPTURING_PHASE || e.bubbles || e.target === g || g instanceof Window) {
                            var k = "function" === d ? b.call(g, e) : b.handleEvent && b.handleEvent(e);
                            g !== this && (h ? (Object.defineProperty(e, "currentTarget", h), h = null) : delete e.currentTarget);
                            return k
                        }
                    };
                    b[nd].push({
                        node: g,
                        type: a,
                        capture: e,
                        once: f,
                        passive: h,
                        fb: k
                    });
                    td[a] ? (this.__handlers =
                        this.__handlers || {}, this.__handlers[a] = this.__handlers[a] || {
                            capture: [],
                            bubble: []
                        }, this.__handlers[a][e ? "capture" : "bubble"].push(k)) : (this instanceof Window ? D.cb : D.addEventListener).call(this, a, k, c)
                }
        }
    }

    function Ad(a, b, c) {
        if (b) {
            if (c && "object" === typeof c) {
                var d = !!c.capture;
                var e = !!c.once;
                var f = !!c.passive
            } else d = !!c, f = e = !1;
            var h = c && c.ea || this,
                g = void 0;
            var k = null;
            try {
                k = b[nd]
            } catch (l) {}
            k && (e = xd(k, h, a, d, e, f), -1 < e && (g = k.splice(e, 1)[0].fb, k.length || (b[nd] = void 0)));
            (this instanceof Window ? D.eb : D.removeEventListener).call(this, a, g || b, c);
            g && td[a] && this.__handlers && this.__handlers[a] && (a = this.__handlers[a][d ? "capture" : "bubble"], g = a.indexOf(g), -1 < g && a.splice(g, 1))
        }
    }

    function Bd() {
        for (var a in td) window.addEventListener(a, function(a) {
            a.__target || (zd(a), wd(a))
        }, !0)
    }

    function zd(a) {
        a.__target = a.target;
        a.ca = a.relatedTarget;
        if (z.w) {
            var b = Object.getPrototypeOf(a);
            if (!b.hasOwnProperty("__patchProto")) {
                var c = Object.create(b);
                c.hb = b;
                Eb(c, rd);
                b.__patchProto = c
            }
            a.__proto__ = b.__patchProto
        } else Eb(a, rd)
    }
    var Cd = sd(window.Event),
        Dd = sd(window.CustomEvent),
        Ed = sd(window.MouseEvent);

    function Fd(a, b) {
        return {
            index: a,
            P: [],
            W: b
        }
    }

    function Gd(a, b, c, d) {
        var e = 0,
            f = 0,
            h = 0,
            g = 0,
            k = Math.min(b - e, d - f);
        if (0 == e && 0 == f) a: {
            for (h = 0; h < k; h++)
                if (a[h] !== c[h]) break a;
            h = k
        }
        if (b == a.length && d == c.length) {
            g = a.length;
            for (var l = c.length, m = 0; m < k - h && Hd(a[--g], c[--l]);) m++;
            g = m
        }
        e += h;
        f += h;
        b -= g;
        d -= g;
        if (0 == b - e && 0 == d - f) return [];
        if (e == b) {
            for (b = Fd(e, 0); f < d;) b.P.push(c[f++]);
            return [b]
        }
        if (f == d) return [Fd(e, b - e)];
        k = e;
        h = f;
        d = d - h + 1;
        g = b - k + 1;
        b = Array(d);
        for (l = 0; l < d; l++) b[l] = Array(g), b[l][0] = l;
        for (l = 0; l < g; l++) b[0][l] = l;
        for (l = 1; l < d; l++)
            for (m = 1; m < g; m++)
                if (a[k + m - 1] === c[h + l - 1]) b[l][m] =
                    b[l - 1][m - 1];
                else {
                    var n = b[l - 1][m] + 1,
                        t = b[l][m - 1] + 1;
                    b[l][m] = n < t ? n : t
                }
        k = b.length - 1;
        h = b[0].length - 1;
        d = b[k][h];
        for (a = []; 0 < k || 0 < h;) 0 == k ? (a.push(2), h--) : 0 == h ? (a.push(3), k--) : (g = b[k - 1][h - 1], l = b[k - 1][h], m = b[k][h - 1], n = l < m ? l < g ? l : g : m < g ? m : g, n == g ? (g == d ? a.push(0) : (a.push(1), d = g), k--, h--) : n == l ? (a.push(3), k--, d = l) : (a.push(2), h--, d = m));
        a.reverse();
        b = void 0;
        k = [];
        for (h = 0; h < a.length; h++) switch (a[h]) {
            case 0:
                b && (k.push(b), b = void 0);
                e++;
                f++;
                break;
            case 1:
                b || (b = Fd(e, 0));
                b.W++;
                e++;
                b.P.push(c[f]);
                f++;
                break;
            case 2:
                b || (b = Fd(e, 0));
                b.W++;
                e++;
                break;
            case 3:
                b || (b = Fd(e, 0)), b.P.push(c[f]), f++
        }
        b && k.push(b);
        return k
    }

    function Hd(a, b) {
        return a === b
    };
    var cd = K.parentNode,
        Id = K.childNodes,
        Jd = {};

    function Kd(a) {
        var b = [];
        do b.unshift(a); while (a = a.parentNode);
        return b
    }

    function Oc(a, b, c) {
        if (a !== Jd) throw new TypeError("Illegal constructor");
        this.Ia = "ShadyRoot";
        a = Id(b);
        this.host = b;
        this.a = c && c.mode;
        Tc(b, a);
        c = y(b);
        c.root = this;
        c.wa = "closed" !== this.a ? this : null;
        c = x(this);
        c.firstChild = c.lastChild = c.parentNode = c.nextSibling = c.previousSibling = null;
        c.childNodes = [];
        this.V = !1;
        this.v = this.j = this.g = null;
        c = 0;
        for (var d = a.length; c < d; c++) D.removeChild.call(b, a[c])
    }

    function Xc(a) {
        a.V || (a.V = !0, Pb(function() {
            return Ld(a)
        }))
    }

    function Ld(a) {
        for (var b; a;) {
            a.V && (b = a);
            a: {
                var c = a;
                a = c.host.getRootNode();
                if (A(a))
                    for (var d = c.host.childNodes, e = 0; e < d.length; e++)
                        if (c = d[e], "slot" == c.localName) break a;
                a = void 0
            }
        }
        b && b._renderRoot()
    }
    Oc.prototype._renderRoot = function() {
        this.V = !1;
        if (this.g) {
            bd(this);
            for (var a = 0, b; a < this.g.length; a++) {
                b = this.g[a];
                var c = y(b),
                    d = c.assignedNodes;
                c.assignedNodes = [];
                c.C = [];
                if (c.ka = d)
                    for (c = 0; c < d.length; c++) {
                        var e = y(d[c]);
                        e.T = e.assignedSlot;
                        e.assignedSlot === b && (e.assignedSlot = null)
                    }
            }
            for (b = this.host.firstChild; b; b = b.nextSibling) Md(this, b);
            for (a = 0; a < this.g.length; a++) {
                b = this.g[a];
                d = y(b);
                if (!d.assignedNodes.length)
                    for (c = b.firstChild; c; c = c.nextSibling) Md(this, c, b);
                (c = (c = y(b.parentNode)) && c.root) && dd(c) && c._renderRoot();
                Nd(this, d.C, d.assignedNodes);
                if (c = d.ka) {
                    for (e = 0; e < c.length; e++) y(c[e]).T = null;
                    d.ka = null;
                    c.length > d.assignedNodes.length && (d.Y = !0)
                }
                d.Y && (d.Y = !1, Od(this, b))
            }
            a = this.g;
            b = [];
            for (d = 0; d < a.length; d++) c = a[d].parentNode, (e = y(c)) && e.root || !(0 > b.indexOf(c)) || b.push(c);
            for (a = 0; a < b.length; a++) {
                d = b[a];
                c = d === this ? this.host : d;
                e = [];
                d = d.childNodes;
                for (var f = 0; f < d.length; f++) {
                    var h = d[f];
                    if ("slot" == h.localName) {
                        h = y(h).C;
                        for (var g = 0; g < h.length; g++) e.push(h[g])
                    } else e.push(h)
                }
                d = void 0;
                f = Id(c);
                h = Gd(e, e.length, f, f.length);
                for (var k = g = 0; g < h.length && (d = h[g]); g++) {
                    for (var l = 0, m; l < d.P.length && (m = d.P[l]); l++) cd(m) === c && D.removeChild.call(c, m), f.splice(d.index + k, 1);
                    k -= d.W
                }
                for (k = 0; k < h.length && (d = h[k]); k++)
                    for (g = f[d.index], l = d.index; l < d.index + d.W; l++) m = e[l], D.insertBefore.call(c, m, g), f.splice(l, 0, m)
            }
        }
    };

    function Md(a, b, c) {
        var d = x(b),
            e = d.T;
        d.T = null;
        c || (c = (a = a.j[b.slot || "__catchall"]) && a[0]);
        c ? (x(c).assignedNodes.push(b), d.assignedSlot = c) : d.assignedSlot = void 0;
        e !== d.assignedSlot && d.assignedSlot && (x(d.assignedSlot).Y = !0)
    }

    function Nd(a, b, c) {
        for (var d = 0, e; d < c.length && (e = c[d]); d++)
            if ("slot" == e.localName) {
                var f = y(e).assignedNodes;
                f && f.length && Nd(a, b, f)
            } else b.push(c[d])
    }

    function Od(a, b) {
        D.dispatchEvent.call(b, new Event("slotchange"));
        b = y(b);
        b.assignedSlot && Od(a, b.assignedSlot)
    }

    function bd(a) {
        if (a.v && a.v.length) {
            for (var b = a.v, c, d = 0; d < b.length; d++) {
                var e = b[d];
                Tc(e);
                Tc(e.parentNode);
                var f = fd(e);
                a.j[f] ? (c = c || {}, c[f] = !0, a.j[f].push(e)) : a.j[f] = [e];
                a.g.push(e)
            }
            if (c)
                for (var h in c) a.j[h] = gd(a.j[h]);
            a.v = []
        }
    }

    function fd(a) {
        var b = a.name || a.getAttribute("name") || "__catchall";
        return a.Da = b
    }

    function gd(a) {
        return a.sort(function(a, c) {
            a = Kd(a);
            for (var b = Kd(c), e = 0; e < a.length; e++) {
                c = a[e];
                var f = b[e];
                if (c !== f) return a = Array.from(c.parentNode.childNodes), a.indexOf(c) - a.indexOf(f)
            }
        })
    }

    function dd(a) {
        bd(a);
        return !(!a.g || !a.g.length)
    };

    function Pd(a) {
        var b = a.getRootNode();
        A(b) && Ld(b);
        return (a = y(a)) && a.assignedSlot || null
    }
    var Qd = {
            addEventListener: yd.bind(window),
            removeEventListener: Ad.bind(window)
        },
        Rd = {
            addEventListener: yd,
            removeEventListener: Ad,
            appendChild: function(a) {
                return Vc(this, a)
            },
            insertBefore: function(a, b) {
                return Vc(this, a, b)
            },
            removeChild: function(a) {
                return Wc(this, a)
            },
            replaceChild: function(a, b) {
                Vc(this, a, b);
                Wc(this, b);
                return a
            },
            cloneNode: function(a) {
                if ("template" == this.localName) var b = D.cloneNode.call(this, a);
                else if (b = D.cloneNode.call(this, !1), a) {
                    a = this.childNodes;
                    for (var c = 0, d; c < a.length; c++) d = a[c].cloneNode(!0),
                        b.appendChild(d)
                }
                return b
            },
            getRootNode: function() {
                return hd(this)
            },
            contains: function(a) {
                return Mb(this, a)
            },
            dispatchEvent: function(a) {
                Qb();
                return D.dispatchEvent.call(this, a)
            }
        };
    Object.defineProperties(Rd, {
        isConnected: {
            get: function() {
                if (Fc && Fc.call(this)) return !0;
                if (this.nodeType == Node.DOCUMENT_FRAGMENT_NODE) return !1;
                var a = this.ownerDocument;
                if (Lb) {
                    if (D.contains.call(a, this)) return !0
                } else if (a.documentElement && D.contains.call(a.documentElement, this)) return !0;
                for (a = this; a && !(a instanceof Document);) a = a.parentNode || (A(a) ? a.host : void 0);
                return !!(a && a instanceof Document)
            },
            configurable: !0
        }
    });
    var Sd = {get assignedSlot() {
                return Pd(this)
            }
        },
        Td = {
            querySelector: function(a) {
                return id(this, function(b) {
                    return Db.call(b, a)
                }, function(a) {
                    return !!a
                })[0] || null
            },
            querySelectorAll: function(a, b) {
                if (b) {
                    b = Array.prototype.slice.call(D.querySelectorAll(this, a));
                    var c = this.getRootNode();
                    return b.filter(function(a) {
                        return a.getRootNode() == c
                    })
                }
                return id(this, function(b) {
                    return Db.call(b, a)
                })
            }
        },
        Ud = {
            assignedNodes: function(a) {
                if ("slot" === this.localName) {
                    var b = this.getRootNode();
                    A(b) && Ld(b);
                    return (b = y(this)) ? (a && a.flatten ?
                        b.C : b.assignedNodes) || [] : []
                }
            }
        },
        Vd = Fb({
            setAttribute: function(a, b) {
                ld(this, a, b)
            },
            removeAttribute: function(a) {
                D.removeAttribute.call(this, a);
                ed(this, a)
            },
            attachShadow: function(a) {
                if (!this) throw "Must provide a host.";
                if (!a) throw "Not enough arguments.";
                return new Oc(Jd, this, a)
            },
            get slot() {
                return this.getAttribute("slot")
            },
            set slot(a) {
                ld(this, "slot", a)
            },
            get assignedSlot() {
                return Pd(this)
            }
        }, Td, Ud);
    Object.defineProperties(Vd, Kc);
    var Wd = Fb({
        importNode: function(a, b) {
            return md(a, b)
        },
        getElementById: function(a) {
            return id(this, function(b) {
                return b.id == a
            }, function(a) {
                return !!a
            })[0] || null
        }
    }, Td);
    Object.defineProperties(Wd, {
        _activeElement: Lc.activeElement
    });
    var Xd = HTMLElement.prototype.blur,
        Yd = Fb({
            blur: function() {
                var a = y(this);
                (a = (a = a && a.root) && a.activeElement) ? a.blur(): Xd.call(this)
            }
        }),
        Zd = {
            addEventListener: function(a, b, c) {
                "object" !== typeof c && (c = {
                    capture: !!c
                });
                c.ea = this;
                this.host.addEventListener(a, b, c)
            },
            removeEventListener: function(a, b, c) {
                "object" !== typeof c && (c = {
                    capture: !!c
                });
                c.ea = this;
                this.host.removeEventListener(a, b, c)
            },
            getElementById: function(a) {
                return id(this, function(b) {
                    return b.id == a
                }, function(a) {
                    return !!a
                })[0] || null
            }
        };

    function M(a, b) {
        for (var c = Object.getOwnPropertyNames(b), d = 0; d < c.length; d++) {
            var e = c[d],
                f = Object.getOwnPropertyDescriptor(b, e);
            f.value ? a[e] = f.value : Object.defineProperty(a, e, f)
        }
    };
    if (z.ua) {
        var ShadyDOM = {
            inUse: z.ua,
            patch: function(a) {
                Qc(a);
                Pc(a);
                return a
            },
            isShadyRoot: A,
            enqueue: Pb,
            flush: Qb,
            settings: z,
            filterMutations: Wb,
            observeChildren: Ub,
            unobserveChildren: Vb,
            nativeMethods: D,
            nativeTree: K
        };
        window.ShadyDOM = ShadyDOM;
        window.Event = Cd;
        window.CustomEvent = Dd;
        window.MouseEvent = Ed;
        Bd();
        var $d = window.customElements && window.customElements.nativeHTMLElement || HTMLElement;
        M(Oc.prototype, Zd);
        M(window.Node.prototype, Rd);
        M(window.Window.prototype, Qd);
        M(window.Text.prototype, Sd);
        M(window.DocumentFragment.prototype,
            Td);
        M(window.Element.prototype, Vd);
        M(window.Document.prototype, Wd);
        window.HTMLSlotElement && M(window.HTMLSlotElement.prototype, Ud);
        M($d.prototype, Yd);
        z.w && (Mc(window.Node.prototype), Mc(window.Text.prototype), Mc(window.DocumentFragment.prototype), Mc(window.Element.prototype), Mc($d.prototype), Mc(window.Document.prototype), window.HTMLSlotElement && Mc(window.HTMLSlotElement.prototype));
        Nc();
        window.ShadowRoot = Oc
    };
    var ae = new Set("annotation-xml color-profile font-face font-face-src font-face-uri font-face-format font-face-name missing-glyph".split(" "));

    function be(a) {
        var b = ae.has(a);
        a = /^[a-z][.0-9_a-z]*-[\-.0-9_a-z]*$/.test(a);
        return !b && a
    }

    function N(a) {
        var b = a.isConnected;
        if (void 0 !== b) return b;
        for (; a && !(a.__CE_isImportDocument || a instanceof Document);) a = a.parentNode || (window.ShadowRoot && a instanceof ShadowRoot ? a.host : void 0);
        return !(!a || !(a.__CE_isImportDocument || a instanceof Document))
    }

    function ce(a, b) {
        for (; b && b !== a && !b.nextSibling;) b = b.parentNode;
        return b && b !== a ? b.nextSibling : null
    }

    function de(a, b, c) {
        c = void 0 === c ? new Set : c;
        for (var d = a; d;) {
            if (d.nodeType === Node.ELEMENT_NODE) {
                var e = d;
                b(e);
                var f = e.localName;
                if ("link" === f && "import" === e.getAttribute("rel")) {
                    d = e.import;
                    if (d instanceof Node && !c.has(d))
                        for (c.add(d), d = d.firstChild; d; d = d.nextSibling) de(d, b, c);
                    d = ce(a, e);
                    continue
                } else if ("template" === f) {
                    d = ce(a, e);
                    continue
                }
                if (e = e.__CE_shadowRoot)
                    for (e = e.firstChild; e; e = e.nextSibling) de(e, b, c)
            }
            d = d.firstChild ? d.firstChild : ce(a, d)
        }
    }

    function O(a, b, c) {
        a[b] = c
    };

    function ee() {
        this.a = new Map;
        this.s = new Map;
        this.i = [];
        this.c = !1
    }

    function fe(a, b, c) {
        a.a.set(b, c);
        a.s.set(c.constructor, c)
    }

    function ge(a, b) {
        a.c = !0;
        a.i.push(b)
    }

    function he(a, b) {
        a.c && de(b, function(b) {
            return a.b(b)
        })
    }
    ee.prototype.b = function(a) {
        if (this.c && !a.__CE_patched) {
            a.__CE_patched = !0;
            for (var b = 0; b < this.i.length; b++) this.i[b](a)
        }
    };

    function Q(a, b) {
        var c = [];
        de(b, function(a) {
            return c.push(a)
        });
        for (b = 0; b < c.length; b++) {
            var d = c[b];
            1 === d.__CE_state ? a.connectedCallback(d) : ie(a, d)
        }
    }

    function R(a, b) {
        var c = [];
        de(b, function(a) {
            return c.push(a)
        });
        for (b = 0; b < c.length; b++) {
            var d = c[b];
            1 === d.__CE_state && a.disconnectedCallback(d)
        }
    }

    function je(a, b, c) {
        c = void 0 === c ? {} : c;
        var d = c.bb || new Set,
            e = c.za || function(b) {
                return ie(a, b)
            },
            f = [];
        de(b, function(b) {
            if ("link" === b.localName && "import" === b.getAttribute("rel")) {
                var c = b.import;
                c instanceof Node && (c.__CE_isImportDocument = !0, c.__CE_hasRegistry = !0);
                c && "complete" === c.readyState ? c.__CE_documentLoadHandled = !0 : b.addEventListener("load", function() {
                    var c = b.import;
                    if (!c.__CE_documentLoadHandled) {
                        c.__CE_documentLoadHandled = !0;
                        var f = new Set(d);
                        f.delete(c);
                        je(a, c, {
                            bb: f,
                            za: e
                        })
                    }
                })
            } else f.push(b)
        }, d);
        if (a.c)
            for (b = 0; b < f.length; b++) a.b(f[b]);
        for (b = 0; b < f.length; b++) e(f[b])
    }

    function ie(a, b) {
        if (void 0 === b.__CE_state) {
            var c = b.ownerDocument;
            if (c.defaultView || c.__CE_isImportDocument && c.__CE_hasRegistry)
                if (c = a.a.get(b.localName)) {
                    c.constructionStack.push(b);
                    var d = c.constructor;
                    try {
                        try {
                            if (new d !== b) throw Error("The custom element constructor did not produce the element being upgraded.");
                        } finally {
                            c.constructionStack.pop()
                        }
                    } catch (h) {
                        throw b.__CE_state = 2, h;
                    }
                    b.__CE_state = 1;
                    b.__CE_definition = c;
                    if (c.attributeChangedCallback)
                        for (c = c.observedAttributes, d = 0; d < c.length; d++) {
                            var e = c[d],
                                f = b.getAttribute(e);
                            null !== f && a.attributeChangedCallback(b, e, null, f, null)
                        }
                    N(b) && a.connectedCallback(b)
                }
        }
    }
    ee.prototype.connectedCallback = function(a) {
        var b = a.__CE_definition;
        b.connectedCallback && b.connectedCallback.call(a)
    };
    ee.prototype.disconnectedCallback = function(a) {
        var b = a.__CE_definition;
        b.disconnectedCallback && b.disconnectedCallback.call(a)
    };
    ee.prototype.attributeChangedCallback = function(a, b, c, d, e) {
        var f = a.__CE_definition;
        f.attributeChangedCallback && -1 < f.observedAttributes.indexOf(b) && f.attributeChangedCallback.call(a, b, c, d, e)
    };

    function ke(a) {
        var b = document;
        this.m = a;
        this.a = b;
        this.G = void 0;
        je(this.m, this.a);
        "loading" === this.a.readyState && (this.G = new MutationObserver(this.b.bind(this)), this.G.observe(this.a, {
            childList: !0,
            subtree: !0
        }))
    }
    ke.prototype.disconnect = function() {
        this.G && this.G.disconnect()
    };
    ke.prototype.b = function(a) {
        var b = this.a.readyState;
        "interactive" !== b && "complete" !== b || this.disconnect();
        for (b = 0; b < a.length; b++)
            for (var c = a[b].addedNodes, d = 0; d < c.length; d++) je(this.m, c[d])
    };

    function le() {
        var a = this;
        this.b = this.a = void 0;
        this.c = new Promise(function(b) {
            a.b = b;
            a.a && b(a.a)
        })
    }
    le.prototype.resolve = function(a) {
        if (this.a) throw Error("Already resolved.");
        this.a = a;
        this.b && this.b(a)
    };

    function S(a) {
        this.ha = !1;
        this.m = a;
        this.la = new Map;
        this.ia = function(a) {
            return a()
        };
        this.S = !1;
        this.ja = [];
        this.Ga = new ke(a)
    }
    S.prototype.define = function(a, b) {
        var c = this;
        if (!(b instanceof Function)) throw new TypeError("Custom element constructors must be functions.");
        if (!be(a)) throw new SyntaxError("The element name '" + a + "' is not valid.");
        if (this.m.a.get(a)) throw Error("A custom element with name '" + a + "' has already been defined.");
        if (this.ha) throw Error("A custom element is already being defined.");
        this.ha = !0;
        try {
            var d = function(a) {
                    var b = e[a];
                    if (void 0 !== b && !(b instanceof Function)) throw Error("The '" + a + "' callback must be a function.");
                    return b
                },
                e = b.prototype;
            if (!(e instanceof Object)) throw new TypeError("The custom element constructor's prototype is not an object.");
            var f = d("connectedCallback");
            var h = d("disconnectedCallback");
            var g = d("adoptedCallback");
            var k = d("attributeChangedCallback");
            var l = b.observedAttributes || []
        } catch (m) {
            return
        } finally {
            this.ha = !1
        }
        b = {
            localName: a,
            constructor: b,
            connectedCallback: f,
            disconnectedCallback: h,
            adoptedCallback: g,
            attributeChangedCallback: k,
            observedAttributes: l,
            constructionStack: []
        };
        fe(this.m, a, b);
        this.ja.push(b);
        this.S || (this.S = !0, this.ia(function() {
            return me(c)
        }))
    };

    function me(a) {
        if (!1 !== a.S) {
            a.S = !1;
            for (var b = a.ja, c = [], d = new Map, e = 0; e < b.length; e++) d.set(b[e].localName, []);
            je(a.m, document, {
                za: function(b) {
                    if (void 0 === b.__CE_state) {
                        var e = b.localName,
                            f = d.get(e);
                        f ? f.push(b) : a.m.a.get(e) && c.push(b)
                    }
                }
            });
            for (e = 0; e < c.length; e++) ie(a.m, c[e]);
            for (; 0 < b.length;) {
                var f = b.shift();
                e = f.localName;
                f = d.get(f.localName);
                for (var h = 0; h < f.length; h++) ie(a.m, f[h]);
                (e = a.la.get(e)) && e.resolve(void 0)
            }
        }
    }
    S.prototype.get = function(a) {
        if (a = this.m.a.get(a)) return a.constructor
    };
    S.prototype.a = function(a) {
        if (!be(a)) return Promise.reject(new SyntaxError("'" + a + "' is not a valid custom element name."));
        var b = this.la.get(a);
        if (b) return b.c;
        b = new le;
        this.la.set(a, b);
        this.m.a.get(a) && !this.ja.some(function(b) {
            return b.localName === a
        }) && b.resolve(void 0);
        return b.c
    };
    S.prototype.b = function(a) {
        this.Ga.disconnect();
        var b = this.ia;
        this.ia = function(c) {
            return a(function() {
                return b(c)
            })
        }
    };
    window.CustomElementRegistry = S;
    S.prototype.define = S.prototype.define;
    S.prototype.get = S.prototype.get;
    S.prototype.whenDefined = S.prototype.a;
    S.prototype.polyfillWrapFlushCallback = S.prototype.b;
    var ne = window.Document.prototype.createElement,
        oe = window.Document.prototype.createElementNS,
        pe = window.Document.prototype.importNode,
        qe = window.Document.prototype.prepend,
        re = window.Document.prototype.append,
        se = window.DocumentFragment.prototype.prepend,
        te = window.DocumentFragment.prototype.append,
        ue = window.Node.prototype.cloneNode,
        ve = window.Node.prototype.appendChild,
        we = window.Node.prototype.insertBefore,
        xe = window.Node.prototype.removeChild,
        ye = window.Node.prototype.replaceChild,
        ze = Object.getOwnPropertyDescriptor(window.Node.prototype,
            "textContent"),
        Ae = window.Element.prototype.attachShadow,
        Be = Object.getOwnPropertyDescriptor(window.Element.prototype, "innerHTML"),
        Ce = window.Element.prototype.getAttribute,
        De = window.Element.prototype.setAttribute,
        Ee = window.Element.prototype.removeAttribute,
        Fe = window.Element.prototype.getAttributeNS,
        Ge = window.Element.prototype.setAttributeNS,
        He = window.Element.prototype.removeAttributeNS,
        Ie = window.Element.prototype.insertAdjacentElement,
        Je = window.Element.prototype.insertAdjacentHTML,
        Ke = window.Element.prototype.prepend,
        Le = window.Element.prototype.append,
        Me = window.Element.prototype.before,
        Ne = window.Element.prototype.after,
        Oe = window.Element.prototype.replaceWith,
        Pe = window.Element.prototype.remove,
        Qe = window.HTMLElement,
        Re = Object.getOwnPropertyDescriptor(window.HTMLElement.prototype, "innerHTML"),
        Se = window.HTMLElement.prototype.insertAdjacentElement,
        Te = window.HTMLElement.prototype.insertAdjacentHTML;
    var Ue = new function() {};

    function Ve() {
        var a = We;
        window.HTMLElement = function() {
            function b() {
                var b = this.constructor,
                    d = a.s.get(b);
                if (!d) throw Error("The custom element being constructed was not registered with `customElements`.");
                var e = d.constructionStack;
                if (0 === e.length) return e = ne.call(document, d.localName), Object.setPrototypeOf(e, b.prototype), e.__CE_state = 1, e.__CE_definition = d, a.b(e), e;
                d = e.length - 1;
                var f = e[d];
                if (f === Ue) throw Error("The HTMLElement constructor was either called reentrantly for this constructor or called multiple times.");
                e[d] = Ue;
                Object.setPrototypeOf(f, b.prototype);
                a.b(f);
                return f
            }
            b.prototype = Qe.prototype;
            return b
        }()
    };

    function Xe(a, b, c) {
        function d(b) {
            return function(c) {
                for (var d = [], e = 0; e < arguments.length; ++e) d[e - 0] = arguments[e];
                e = [];
                for (var f = [], l = 0; l < d.length; l++) {
                    var m = d[l];
                    m instanceof Element && N(m) && f.push(m);
                    if (m instanceof DocumentFragment)
                        for (m = m.firstChild; m; m = m.nextSibling) e.push(m);
                    else e.push(m)
                }
                b.apply(this, d);
                for (d = 0; d < f.length; d++) R(a, f[d]);
                if (N(this))
                    for (d = 0; d < e.length; d++) f = e[d], f instanceof Element && Q(a, f)
            }
        }
        void 0 !== c.$ && (b.prepend = d(c.$));
        void 0 !== c.append && (b.append = d(c.append))
    };

    function Ye() {
        var a = We;
        O(Document.prototype, "createElement", function(b) {
            if (this.__CE_hasRegistry) {
                var c = a.a.get(b);
                if (c) return new c.constructor
            }
            b = ne.call(this, b);
            a.b(b);
            return b
        });
        O(Document.prototype, "importNode", function(b, c) {
            b = pe.call(this, b, c);
            this.__CE_hasRegistry ? je(a, b) : he(a, b);
            return b
        });
        O(Document.prototype, "createElementNS", function(b, c) {
            if (this.__CE_hasRegistry && (null === b || "http://www.w3.org/1999/xhtml" === b)) {
                var d = a.a.get(c);
                if (d) return new d.constructor
            }
            b = oe.call(this, b, c);
            a.b(b);
            return b
        });
        Xe(a, Document.prototype, {
            $: qe,
            append: re
        })
    };

    function Ze() {
        var a = We;

        function b(b, d) {
            Object.defineProperty(b, "textContent", {
                enumerable: d.enumerable,
                configurable: !0,
                get: d.get,
                set: function(b) {
                    if (this.nodeType === Node.TEXT_NODE) d.set.call(this, b);
                    else {
                        var c = void 0;
                        if (this.firstChild) {
                            var e = this.childNodes,
                                g = e.length;
                            if (0 < g && N(this)) {
                                c = Array(g);
                                for (var k = 0; k < g; k++) c[k] = e[k]
                            }
                        }
                        d.set.call(this, b);
                        if (c)
                            for (b = 0; b < c.length; b++) R(a, c[b])
                    }
                }
            })
        }
        O(Node.prototype, "insertBefore", function(b, d) {
            if (b instanceof DocumentFragment) {
                var c = Array.prototype.slice.apply(b.childNodes);
                b = we.call(this, b, d);
                if (N(this))
                    for (d = 0; d < c.length; d++) Q(a, c[d]);
                return b
            }
            c = N(b);
            d = we.call(this, b, d);
            c && R(a, b);
            N(this) && Q(a, b);
            return d
        });
        O(Node.prototype, "appendChild", function(b) {
            if (b instanceof DocumentFragment) {
                var c = Array.prototype.slice.apply(b.childNodes);
                b = ve.call(this, b);
                if (N(this))
                    for (var e = 0; e < c.length; e++) Q(a, c[e]);
                return b
            }
            c = N(b);
            e = ve.call(this, b);
            c && R(a, b);
            N(this) && Q(a, b);
            return e
        });
        O(Node.prototype, "cloneNode", function(b) {
            b = ue.call(this, b);
            this.ownerDocument.__CE_hasRegistry ? je(a, b) :
                he(a, b);
            return b
        });
        O(Node.prototype, "removeChild", function(b) {
            var c = N(b),
                e = xe.call(this, b);
            c && R(a, b);
            return e
        });
        O(Node.prototype, "replaceChild", function(b, d) {
            if (b instanceof DocumentFragment) {
                var c = Array.prototype.slice.apply(b.childNodes);
                b = ye.call(this, b, d);
                if (N(this))
                    for (R(a, d), d = 0; d < c.length; d++) Q(a, c[d]);
                return b
            }
            c = N(b);
            var f = ye.call(this, b, d),
                h = N(this);
            h && R(a, d);
            c && R(a, b);
            h && Q(a, b);
            return f
        });
        ze && ze.get ? b(Node.prototype, ze) : ge(a, function(a) {
            b(a, {
                enumerable: !0,
                configurable: !0,
                get: function() {
                    for (var a = [], b = 0; b < this.childNodes.length; b++) a.push(this.childNodes[b].textContent);
                    return a.join("")
                },
                set: function(a) {
                    for (; this.firstChild;) xe.call(this, this.firstChild);
                    ve.call(this, document.createTextNode(a))
                }
            })
        })
    };

    function $e(a) {
        var b = Element.prototype;

        function c(b) {
            return function(c) {
                for (var d = [], e = 0; e < arguments.length; ++e) d[e - 0] = arguments[e];
                e = [];
                for (var g = [], k = 0; k < d.length; k++) {
                    var l = d[k];
                    l instanceof Element && N(l) && g.push(l);
                    if (l instanceof DocumentFragment)
                        for (l = l.firstChild; l; l = l.nextSibling) e.push(l);
                    else e.push(l)
                }
                b.apply(this, d);
                for (d = 0; d < g.length; d++) R(a, g[d]);
                if (N(this))
                    for (d = 0; d < e.length; d++) g = e[d], g instanceof Element && Q(a, g)
            }
        }
        void 0 !== Me && (b.before = c(Me));
        void 0 !== Me && (b.after = c(Ne));
        void 0 !==
            Oe && O(b, "replaceWith", function(b) {
                for (var c = [], d = 0; d < arguments.length; ++d) c[d - 0] = arguments[d];
                d = [];
                for (var h = [], g = 0; g < c.length; g++) {
                    var k = c[g];
                    k instanceof Element && N(k) && h.push(k);
                    if (k instanceof DocumentFragment)
                        for (k = k.firstChild; k; k = k.nextSibling) d.push(k);
                    else d.push(k)
                }
                g = N(this);
                Oe.apply(this, c);
                for (c = 0; c < h.length; c++) R(a, h[c]);
                if (g)
                    for (R(a, this), c = 0; c < d.length; c++) h = d[c], h instanceof Element && Q(a, h)
            });
        void 0 !== Pe && O(b, "remove", function() {
            var b = N(this);
            Pe.call(this);
            b && R(a, this)
        })
    };

    function af() {
        var a = We;

        function b(b, c) {
            Object.defineProperty(b, "innerHTML", {
                enumerable: c.enumerable,
                configurable: !0,
                get: c.get,
                set: function(b) {
                    var d = this,
                        e = void 0;
                    N(this) && (e = [], de(this, function(a) {
                        a !== d && e.push(a)
                    }));
                    c.set.call(this, b);
                    if (e)
                        for (var f = 0; f < e.length; f++) {
                            var h = e[f];
                            1 === h.__CE_state && a.disconnectedCallback(h)
                        }
                    this.ownerDocument.__CE_hasRegistry ? je(a, this) : he(a, this);
                    return b
                }
            })
        }

        function c(b, c) {
            O(b, "insertAdjacentElement", function(b, d) {
                var e = N(d);
                b = c.call(this, b, d);
                e && R(a, d);
                N(b) && Q(a,
                    d);
                return b
            })
        }

        function d(b, c) {
            function d(b, c) {
                for (var d = []; b !== c; b = b.nextSibling) d.push(b);
                for (c = 0; c < d.length; c++) je(a, d[c])
            }
            O(b, "insertAdjacentHTML", function(a, b) {
                a = a.toLowerCase();
                if ("beforebegin" === a) {
                    var e = this.previousSibling;
                    c.call(this, a, b);
                    d(e || this.parentNode.firstChild, this)
                } else if ("afterbegin" === a) e = this.firstChild, c.call(this, a, b), d(this.firstChild, e);
                else if ("beforeend" === a) e = this.lastChild, c.call(this, a, b), d(e || this.firstChild, null);
                else if ("afterend" === a) e = this.nextSibling, c.call(this,
                    a, b), d(this.nextSibling, e);
                else throw new SyntaxError("The value provided (" + String(a) + ") is not one of 'beforebegin', 'afterbegin', 'beforeend', or 'afterend'.");
            })
        }
        Ae && O(Element.prototype, "attachShadow", function(a) {
            return this.__CE_shadowRoot = a = Ae.call(this, a)
        });
        Be && Be.get ? b(Element.prototype, Be) : Re && Re.get ? b(HTMLElement.prototype, Re) : ge(a, function(a) {
            b(a, {
                enumerable: !0,
                configurable: !0,
                get: function() {
                    return ue.call(this, !0).innerHTML
                },
                set: function(a) {
                    var b = "template" === this.localName,
                        c = b ? this.content :
                        this,
                        d = ne.call(document, this.localName);
                    for (d.innerHTML = a; 0 < c.childNodes.length;) xe.call(c, c.childNodes[0]);
                    for (a = b ? d.content : d; 0 < a.childNodes.length;) ve.call(c, a.childNodes[0])
                }
            })
        });
        O(Element.prototype, "setAttribute", function(b, c) {
            if (1 !== this.__CE_state) return De.call(this, b, c);
            var d = Ce.call(this, b);
            De.call(this, b, c);
            c = Ce.call(this, b);
            a.attributeChangedCallback(this, b, d, c, null)
        });
        O(Element.prototype, "setAttributeNS", function(b, c, d) {
            if (1 !== this.__CE_state) return Ge.call(this, b, c, d);
            var e = Fe.call(this,
                b, c);
            Ge.call(this, b, c, d);
            d = Fe.call(this, b, c);
            a.attributeChangedCallback(this, c, e, d, b)
        });
        O(Element.prototype, "removeAttribute", function(b) {
            if (1 !== this.__CE_state) return Ee.call(this, b);
            var c = Ce.call(this, b);
            Ee.call(this, b);
            null !== c && a.attributeChangedCallback(this, b, c, null, null)
        });
        O(Element.prototype, "removeAttributeNS", function(b, c) {
            if (1 !== this.__CE_state) return He.call(this, b, c);
            var d = Fe.call(this, b, c);
            He.call(this, b, c);
            var e = Fe.call(this, b, c);
            d !== e && a.attributeChangedCallback(this, c, d, e, b)
        });
        Se ?
            c(HTMLElement.prototype, Se) : Ie ? c(Element.prototype, Ie) : console.warn("Custom Elements: `Element#insertAdjacentElement` was not patched.");
        Te ? d(HTMLElement.prototype, Te) : Je ? d(Element.prototype, Je) : console.warn("Custom Elements: `Element#insertAdjacentHTML` was not patched.");
        Xe(a, Element.prototype, {
            $: Ke,
            append: Le
        });
        $e(a)
    };
    var bf = window.customElements;
    if (!bf || bf.forcePolyfill || "function" != typeof bf.define || "function" != typeof bf.get) {
        var We = new ee;
        Ve();
        Ye();
        Xe(We, DocumentFragment.prototype, {
            $: se,
            append: te
        });
        Ze();
        af();
        document.__CE_hasRegistry = !0;
        var customElements = new S(We);
        Object.defineProperty(window, "customElements", {
            configurable: !0,
            enumerable: !0,
            value: customElements
        })
    };

    function cf() {
        this.end = this.start = 0;
        this.rules = this.parent = this.previous = null;
        this.cssText = this.parsedCssText = "";
        this.atRule = !1;
        this.type = 0;
        this.parsedSelector = this.selector = this.keyframesName = ""
    }

    function df(a) {
        a = a.replace(ef, "").replace(ff, "");
        var b = jf,
            c = a,
            d = new cf;
        d.start = 0;
        d.end = c.length;
        for (var e = d, f = 0, h = c.length; f < h; f++)
            if ("{" === c[f]) {
                e.rules || (e.rules = []);
                var g = e,
                    k = g.rules[g.rules.length - 1] || null;
                e = new cf;
                e.start = f + 1;
                e.parent = g;
                e.previous = k;
                g.rules.push(e)
            } else "}" === c[f] && (e.end = f + 1, e = e.parent || d);
        return b(d, a)
    }

    function jf(a, b) {
        var c = b.substring(a.start, a.end - 1);
        a.parsedCssText = a.cssText = c.trim();
        a.parent && (c = b.substring(a.previous ? a.previous.end : a.parent.start, a.start - 1), c = kf(c), c = c.replace(lf, " "), c = c.substring(c.lastIndexOf(";") + 1), c = a.parsedSelector = a.selector = c.trim(), a.atRule = 0 === c.indexOf("@"), a.atRule ? 0 === c.indexOf("@media") ? a.type = mf : c.match(nf) && (a.type = of, a.keyframesName = a.selector.split(lf).pop()) : a.type = 0 === c.indexOf("--") ? pf : qf);
        if (c = a.rules)
            for (var d = 0, e = c.length, f; d < e && (f = c[d]); d++) jf(f,
                b);
        return a
    }

    function kf(a) {
        return a.replace(/\\([0-9a-f]{1,6})\s/gi, function(a, c) {
            a = c;
            for (c = 6 - a.length; c--;) a = "0" + a;
            return "\\" + a
        })
    }

    function rf(a, b, c) {
        c = void 0 === c ? "" : c;
        var d = "";
        if (a.cssText || a.rules) {
            var e = a.rules,
                f;
            if (f = e) f = e[0], f = !(f && f.selector && 0 === f.selector.indexOf("--"));
            if (f) {
                f = 0;
                for (var h = e.length, g; f < h && (g = e[f]); f++) d = rf(g, b, d)
            } else b ? b = a.cssText : (b = a.cssText, b = b.replace(sf, "").replace(tf, ""), b = b.replace(uf, "").replace(vf, "")), (d = b.trim()) && (d = "  " + d + "\n")
        }
        d && (a.selector && (c += a.selector + " {\n"), c += d, a.selector && (c += "}\n\n"));
        return c
    }
    var qf = 1,
        of = 7,
        mf = 4,
        pf = 1E3,
        ef = /\/\*[^*]*\*+([^/*][^*]*\*+)*\//gim,
        ff = /@import[^;]*;/gim,
        sf = /(?:^[^;\-\s}]+)?--[^;{}]*?:[^{};]*?(?:[;\n]|$)/gim,
        tf = /(?:^[^;\-\s}]+)?--[^;{}]*?:[^{};]*?{[^}]*?}(?:[;\n]|$)?/gim,
        uf = /@apply\s*\(?[^);]*\)?\s*(?:[;\n]|$)?/gim,
        vf = /[^;:]*?:[^;]*?var\([^;]*\)(?:[;\n]|$)?/gim,
        nf = /^@[^\s]*keyframes/,
        lf = /\s+/g;
    var T = !(window.ShadyDOM && window.ShadyDOM.inUse),
        wf;

    function xf(a) {
        wf = a && a.shimcssproperties ? !1 : T || !(navigator.userAgent.match(/AppleWebKit\/601|Edge\/15/) || !window.CSS || !CSS.supports || !CSS.supports("box-shadow", "0 0 0 var(--foo)"))
    }
    window.ShadyCSS && void 0 !== window.ShadyCSS.nativeCss ? wf = window.ShadyCSS.nativeCss : window.ShadyCSS ? (xf(window.ShadyCSS), window.ShadyCSS = void 0) : xf(window.WebComponents && window.WebComponents.flags);
    var U = wf;
    var yf = /(?:^|[;\s{]\s*)(--[\w-]*?)\s*:\s*(?:((?:'(?:\\'|.)*?'|"(?:\\"|.)*?"|\([^)]*?\)|[^};{])+)|\{([^}]*)\}(?:(?=[;\s}])|$))/gi,
        zf = /(?:^|\W+)@apply\s*\(?([^);\n]*)\)?/gi,
        Af = /(--[\w-]+)\s*([:,;)]|$)/gi,
        Bf = /(animation\s*:)|(animation-name\s*:)/,
        Cf = /@media\s(.*)/,
        Df = /\{[^}]*\}/g;
    var Ef = new Set;

    function Ff(a, b) {
        if (!a) return "";
        "string" === typeof a && (a = df(a));
        b && Gf(a, b);
        return rf(a, U)
    }

    function Hf(a) {
        !a.__cssRules && a.textContent && (a.__cssRules = df(a.textContent));
        return a.__cssRules || null
    }

    function If(a) {
        return !!a.parent && a.parent.type === of
    }

    function Gf(a, b, c, d) {
        if (a) {
            var e = !1,
                f = a.type;
            if (d && f === mf) {
                var h = a.selector.match(Cf);
                h && (window.matchMedia(h[1]).matches || (e = !0))
            }
            f === qf ? b(a) : c && f === of ? c(a) : f === pf && (e = !0);
            if ((a = a.rules) && !e) {
                e = 0;
                f = a.length;
                for (var g; e < f && (g = a[e]); e++) Gf(g, b, c, d)
            }
        }
    }

    function Jf(a, b, c, d) {
        var e = document.createElement("style");
        b && e.setAttribute("scope", b);
        e.textContent = a;
        Kf(e, c, d);
        return e
    }
    var Lf = null;

    function Kf(a, b, c) {
        b = b || document.head;
        b.insertBefore(a, c && c.nextSibling || b.firstChild);
        Lf ? a.compareDocumentPosition(Lf) === Node.DOCUMENT_POSITION_PRECEDING && (Lf = a) : Lf = a
    }

    function Mf(a, b) {
        var c = a.indexOf("var(");
        if (-1 === c) return b(a, "", "", "");
        a: {
            var d = 0;
            var e = c + 3;
            for (var f = a.length; e < f; e++)
                if ("(" === a[e]) d++;
                else if (")" === a[e] && 0 === --d) break a;
            e = -1
        }
        d = a.substring(c + 4, e);
        c = a.substring(0, c);
        a = Mf(a.substring(e + 1), b);
        e = d.indexOf(",");
        return -1 === e ? b(c, d.trim(), "", a) : b(c, d.substring(0, e).trim(), d.substring(e + 1).trim(), a)
    }

    function Nf(a, b) {
        T ? a.setAttribute("class", b) : window.ShadyDOM.nativeMethods.setAttribute.call(a, "class", b)
    }

    function Of(a) {
        var b = a.localName,
            c = "";
        b ? -1 < b.indexOf("-") || (c = b, b = a.getAttribute && a.getAttribute("is") || "") : (b = a.is, c = a.extends);
        return {
            is: b,
            R: c
        }
    };

    function Pf() {}

    function Qf(a, b, c) {
        var d = V;
        a.__styleScoped ? a.__styleScoped = null : Rf(d, a, b || "", c)
    }

    function Rf(a, b, c, d) {
        b.nodeType === Node.ELEMENT_NODE && Sf(b, c, d);
        if (b = "template" === b.localName ? (b.content || b.ib).childNodes : b.children || b.childNodes)
            for (var e = 0; e < b.length; e++) Rf(a, b[e], c, d)
    }

    function Sf(a, b, c) {
        if (b)
            if (a.classList) c ? (a.classList.remove("style-scope"), a.classList.remove(b)) : (a.classList.add("style-scope"), a.classList.add(b));
            else if (a.getAttribute) {
            var d = a.getAttribute(Tf);
            c ? d && (b = d.replace("style-scope", "").replace(b, ""), Nf(a, b)) : Nf(a, (d ? d + " " : "") + "style-scope " + b)
        }
    }

    function Uf(a, b, c) {
        var d = V,
            e = a.__cssBuild;
        T || "shady" === e ? b = Ff(b, c) : (a = Of(a), b = Vf(d, b, a.is, a.R, c) + "\n\n");
        return b.trim()
    }

    function Vf(a, b, c, d, e) {
        var f = Wf(c, d);
        c = c ? Xf + c : "";
        return Ff(b, function(b) {
            b.c || (b.selector = b.o = Yf(a, b, a.b, c, f), b.c = !0);
            e && e(b, c, f)
        })
    }

    function Wf(a, b) {
        return b ? "[is=" + a + "]" : a
    }

    function Yf(a, b, c, d, e) {
        var f = b.selector.split(Zf);
        if (!If(b)) {
            b = 0;
            for (var h = f.length, g; b < h && (g = f[b]); b++) f[b] = c.call(a, g, d, e)
        }
        return f.join(Zf)
    }

    function $f(a) {
        return a.replace(ag, function(a, c, d) {
            -1 < d.indexOf("+") ? d = d.replace(/\+/g, "___") : -1 < d.indexOf("___") && (d = d.replace(/___/g, "+"));
            return ":" + c + "(" + d + ")"
        })
    }
    Pf.prototype.b = function(a, b, c) {
        var d = !1;
        a = a.trim();
        var e = ag.test(a);
        e && (a = a.replace(ag, function(a, b, c) {
            return ":" + b + "(" + c.replace(/\s/g, "") + ")"
        }), a = $f(a));
        a = a.replace(bg, cg + " $1");
        a = a.replace(dg, function(a, e, g) {
            d || (a = eg(g, e, b, c), d = d || a.stop, e = a.Na, g = a.value);
            return e + g
        });
        e && (a = $f(a));
        return a
    };

    function eg(a, b, c, d) {
        var e = a.indexOf(fg);
        0 <= a.indexOf(cg) ? a = gg(a, d) : 0 !== e && (a = c ? hg(a, c) : a);
        c = !1;
        0 <= e && (b = "", c = !0);
        if (c) {
            var f = !0;
            c && (a = a.replace(ig, function(a, b) {
                return " > " + b
            }))
        }
        a = a.replace(jg, function(a, b, c) {
            return '[dir="' + c + '"] ' + b + ", " + b + '[dir="' + c + '"]'
        });
        return {
            value: a,
            Na: b,
            stop: f
        }
    }

    function hg(a, b) {
        a = a.split(kg);
        a[0] += b;
        return a.join(kg)
    }

    function gg(a, b) {
        var c = a.match(lg);
        return (c = c && c[2].trim() || "") ? c[0].match(mg) ? a.replace(lg, function(a, c, f) {
            return b + f
        }) : c.split(mg)[0] === b ? c : ng : a.replace(cg, b)
    }

    function og(a) {
        a.selector === pg && (a.selector = "html")
    }
    Pf.prototype.c = function(a) {
        return a.match(fg) ? this.b(a, qg) : hg(a.trim(), qg)
    };
    q.Object.defineProperties(Pf.prototype, {
        a: {
            configurable: !0,
            enumerable: !0,
            get: function() {
                return "style-scope"
            }
        }
    });
    var ag = /:(nth[-\w]+)\(([^)]+)\)/,
        qg = ":not(.style-scope)",
        Zf = ",",
        dg = /(^|[\s>+~]+)((?:\[.+?\]|[^\s>+~=[])+)/g,
        mg = /[[.:#*]/,
        cg = ":host",
        pg = ":root",
        fg = "::slotted",
        bg = new RegExp("^(" + fg + ")"),
        lg = /(:host)(?:\(((?:\([^)(]*\)|[^)(]*)+?)\))/,
        ig = /(?:::slotted)(?:\(((?:\([^)(]*\)|[^)(]*)+?)\))/,
        jg = /(.*):dir\((?:(ltr|rtl))\)/,
        Xf = ".",
        kg = ":",
        Tf = "class",
        ng = "should_not_match",
        V = new Pf;

    function rg(a, b, c, d) {
        this.B = a || null;
        this.b = b || null;
        this.ma = c || [];
        this.K = null;
        this.R = d || "";
        this.a = this.u = this.F = null
    }

    function W(a) {
        return a ? a.__styleInfo : null
    }

    function sg(a, b) {
        return a.__styleInfo = b
    }
    rg.prototype.c = function() {
        return this.B
    };
    rg.prototype._getStyleRules = rg.prototype.c;

    function tg(a) {
        var b = this.matches || this.matchesSelector || this.mozMatchesSelector || this.msMatchesSelector || this.oMatchesSelector || this.webkitMatchesSelector;
        return b && b.call(this, a)
    }
    var ug = navigator.userAgent.match("Trident");

    function vg() {}

    function wg(a) {
        var b = {},
            c = [],
            d = 0;
        Gf(a, function(a) {
            xg(a);
            a.index = d++;
            a = a.l.cssText;
            for (var c; c = Af.exec(a);) {
                var e = c[1];
                ":" !== c[2] && (b[e] = !0)
            }
        }, function(a) {
            c.push(a)
        });
        a.b = c;
        a = [];
        for (var e in b) a.push(e);
        return a
    }

    function xg(a) {
        if (!a.l) {
            var b = {},
                c = {};
            yg(a, c) && (b.A = c, a.rules = null);
            b.cssText = a.parsedCssText.replace(Df, "").replace(yf, "");
            a.l = b
        }
    }

    function yg(a, b) {
        var c = a.l;
        if (c) {
            if (c.A) return Object.assign(b, c.A), !0
        } else {
            c = a.parsedCssText;
            for (var d; a = yf.exec(c);) {
                d = (a[2] || a[3]).trim();
                if ("inherit" !== d || "unset" !== d) b[a[1].trim()] = d;
                d = !0
            }
            return d
        }
    }

    function zg(a, b, c) {
        b && (b = 0 <= b.indexOf(";") ? Ag(a, b, c) : Mf(b, function(b, e, f, h) {
            if (!e) return b + h;
            (e = zg(a, c[e], c)) && "initial" !== e ? "apply-shim-inherit" === e && (e = "inherit") : e = zg(a, c[f] || f, c) || f;
            return b + (e || "") + h
        }));
        return b && b.trim() || ""
    }

    function Ag(a, b, c) {
        b = b.split(";");
        for (var d = 0, e, f; d < b.length; d++)
            if (e = b[d]) {
                zf.lastIndex = 0;
                if (f = zf.exec(e)) e = zg(a, c[f[1]], c);
                else if (f = e.indexOf(":"), -1 !== f) {
                    var h = e.substring(f);
                    h = h.trim();
                    h = zg(a, h, c) || h;
                    e = e.substring(0, f) + h
                }
                b[d] = e && e.lastIndexOf(";") === e.length - 1 ? e.slice(0, -1) : e || ""
            }
        return b.join(";")
    }

    function Bg(a, b) {
        var c = {},
            d = [];
        Gf(a, function(a) {
            a.l || xg(a);
            var e = a.o || a.parsedSelector;
            b && a.l.A && e && tg.call(b, e) && (yg(a, c), a = a.index, e = parseInt(a / 32, 10), d[e] = (d[e] || 0) | 1 << a % 32)
        }, null, !0);
        return {
            A: c,
            key: d
        }
    }

    function Cg(a, b, c, d) {
        b.l || xg(b);
        if (b.l.A) {
            var e = Of(a);
            a = e.is;
            e = e.R;
            e = a ? Wf(a, e) : "html";
            var f = b.parsedSelector,
                h = ":host > *" === f || "html" === f,
                g = 0 === f.indexOf(":host") && !h;
            "shady" === c && (h = f === e + " > *." + e || -1 !== f.indexOf("html"), g = !h && 0 === f.indexOf(e));
            "shadow" === c && (h = ":host > *" === f || "html" === f, g = g && !h);
            if (h || g) c = e, g && (b.o || (b.o = Yf(V, b, V.b, a ? Xf + a : "", e)), c = b.o || e), d({
                Za: c,
                Ta: g,
                rb: h
            })
        }
    }

    function Dg(a, b) {
        var c = {},
            d = {},
            e = b && b.__cssBuild;
        Gf(b, function(b) {
            Cg(a, b, e, function(e) {
                tg.call(a.b || a, e.Za) && (e.Ta ? yg(b, c) : yg(b, d))
            })
        }, null, !0);
        return {
            Ya: d,
            Ra: c
        }
    }

    function Eg(a, b, c, d) {
        var e = Of(b),
            f = Wf(e.is, e.R),
            h = new RegExp("(?:^|[^.#[:])" + (b.extends ? "\\" + f.slice(0, -1) + "\\]" : f) + "($|[.:[\\s>+~])");
        e = W(b).B;
        var g = Fg(e, d);
        return Uf(b, e, function(b) {
            var e = "";
            b.l || xg(b);
            b.l.cssText && (e = Ag(a, b.l.cssText, c));
            b.cssText = e;
            if (!T && !If(b) && b.cssText) {
                var k = e = b.cssText;
                null == b.ta && (b.ta = Bf.test(e));
                if (b.ta)
                    if (null == b.Z) {
                        b.Z = [];
                        for (var n in g) k = g[n], k = k(e), e !== k && (e = k, b.Z.push(n))
                    } else {
                        for (n = 0; n < b.Z.length; ++n) k = g[b.Z[n]], e = k(e);
                        k = e
                    }
                b.cssText = k;
                b.o = b.o || b.selector;
                e = "." +
                    d;
                n = b.o.split(",");
                k = 0;
                for (var t = n.length, B; k < t && (B = n[k]); k++) n[k] = B.match(h) ? B.replace(f, e) : e + " " + B;
                b.selector = n.join(",")
            }
        })
    }

    function Fg(a, b) {
        a = a.b;
        var c = {};
        if (!T && a)
            for (var d = 0, e = a[d]; d < a.length; e = a[++d]) {
                var f = e,
                    h = b;
                f.i = new RegExp("\\b" + f.keyframesName + "(?!\\B|-)", "g");
                f.a = f.keyframesName + "-" + h;
                f.o = f.o || f.selector;
                f.selector = f.o.replace(f.keyframesName, f.a);
                c[e.keyframesName] = Gg(e)
            }
        return c
    }

    function Gg(a) {
        return function(b) {
            return b.replace(a.i, a.a)
        }
    }

    function Hg(a, b) {
        var c = Ig,
            d = Hf(a);
        a.textContent = Ff(d, function(a) {
            var d = a.cssText = a.parsedCssText;
            a.l && a.l.cssText && (d = d.replace(sf, "").replace(tf, ""), a.cssText = Ag(c, d, b))
        })
    }
    q.Object.defineProperties(vg.prototype, {
        a: {
            configurable: !0,
            enumerable: !0,
            get: function() {
                return "x-scope"
            }
        }
    });
    var Ig = new vg;
    var Jg = {},
        Kg = window.customElements;
    if (Kg && !T) {
        var Lg = Kg.define;
        Kg.define = function(a, b, c) {
            var d = document.createComment(" Shady DOM styles for " + a + " "),
                e = document.head;
            e.insertBefore(d, (Lf ? Lf.nextSibling : null) || e.firstChild);
            Lf = d;
            Jg[a] = d;
            return Lg.call(Kg, a, b, c)
        }
    };

    function Mg() {
        this.cache = {}
    }
    Mg.prototype.store = function(a, b, c, d) {
        var e = this.cache[a] || [];
        e.push({
            A: b,
            styleElement: c,
            u: d
        });
        100 < e.length && e.shift();
        this.cache[a] = e
    };
    Mg.prototype.fetch = function(a, b, c) {
        if (a = this.cache[a])
            for (var d = a.length - 1; 0 <= d; d--) {
                var e = a[d],
                    f;
                a: {
                    for (f = 0; f < c.length; f++) {
                        var h = c[f];
                        if (e.A[h] !== b[h]) {
                            f = !1;
                            break a
                        }
                    }
                    f = !0
                }
                if (f) return e
            }
    };

    function Ng() {}

    function Og(a) {
        for (var b = 0; b < a.length; b++) {
            var c = a[b];
            if (c.target !== document.documentElement && c.target !== document.head)
                for (var d = 0; d < c.addedNodes.length; d++) {
                    var e = c.addedNodes[d];
                    if (e.nodeType === Node.ELEMENT_NODE) {
                        var f = e.getRootNode();
                        var h = e;
                        var g = [];
                        h.classList ? g = Array.from(h.classList) : h instanceof window.SVGElement && h.hasAttribute("class") && (g = h.getAttribute("class").split(/\s+/));
                        h = g;
                        g = h.indexOf(V.a);
                        if ((h = -1 < g ? h[g + 1] : "") && f === e.ownerDocument) Qf(e, h, !0);
                        else if (f.nodeType === Node.DOCUMENT_FRAGMENT_NODE &&
                            (f = f.host))
                            if (f = Of(f).is, h === f)
                                for (e = window.ShadyDOM.nativeMethods.querySelectorAll.call(e, ":not(." + V.a + ")"), f = 0; f < e.length; f++) Sf(e[f], h);
                            else h && Qf(e, h, !0), Qf(e, f)
                    }
                }
        }
    }
    if (!T) {
        var Pg = new MutationObserver(Og),
            Qg = function(a) {
                Pg.observe(a, {
                    childList: !0,
                    subtree: !0
                })
            };
        if (window.customElements && !window.customElements.polyfillWrapFlushCallback) Qg(document);
        else {
            var Rg = function() {
                Qg(document.body)
            };
            window.HTMLImports ? window.HTMLImports.whenReady(Rg) : requestAnimationFrame(function() {
                if ("loading" === document.readyState) {
                    var a = function() {
                        Rg();
                        document.removeEventListener("readystatechange", a)
                    };
                    document.addEventListener("readystatechange", a)
                } else Rg()
            })
        }
        Ng = function() {
            Og(Pg.takeRecords())
        }
    }
    var Sg = Ng;
    var Tg = {};
    var Ug = Promise.resolve();

    function Vg(a) {
        if (a = Tg[a]) a._applyShimCurrentVersion = a._applyShimCurrentVersion || 0, a._applyShimValidatingVersion = a._applyShimValidatingVersion || 0, a._applyShimNextVersion = (a._applyShimNextVersion || 0) + 1
    }

    function Wg(a) {
        return a._applyShimCurrentVersion === a._applyShimNextVersion
    }

    function Xg(a) {
        a._applyShimValidatingVersion = a._applyShimNextVersion;
        a.sa || (a.sa = !0, Ug.then(function() {
            a._applyShimCurrentVersion = a._applyShimNextVersion;
            a.sa = !1
        }))
    };
    var Yg = null,
        Zg = window.HTMLImports && window.HTMLImports.whenReady || null,
        $g;

    function ah(a) {
        requestAnimationFrame(function() {
            Zg ? Zg(a) : (Yg || (Yg = new Promise(function(a) {
                $g = a
            }), "complete" === document.readyState ? $g() : document.addEventListener("readystatechange", function() {
                "complete" === document.readyState && $g()
            })), Yg.then(function() {
                a && a()
            }))
        })
    };
    var bh = new Mg;

    function X() {
        var a = this;
        this.N = {};
        this.c = document.documentElement;
        var b = new cf;
        b.rules = [];
        this.i = sg(this.c, new rg(b));
        this.s = !1;
        this.b = this.a = null;
        ah(function() {
            ch(a)
        })
    }
    p = X.prototype;
    p.Ba = function() {
        Sg()
    };
    p.Pa = function(a) {
        return Hf(a)
    };
    p.ab = function(a) {
        return Ff(a)
    };
    p.prepareTemplate = function(a, b, c) {
        if (!a.La) {
            a.La = !0;
            a.name = b;
            a.extends = c;
            Tg[b] = a;
            var d = (d = a.content.querySelector("style")) ? d.getAttribute("css-build") || "" : "";
            var e = [];
            for (var f = a.content.querySelectorAll("style"), h = 0; h < f.length; h++) {
                var g = f[h];
                if (g.hasAttribute("shady-unscoped")) {
                    if (!T) {
                        var k = g.textContent;
                        Ef.has(k) || (Ef.add(k), k = g.cloneNode(!0), document.head.appendChild(k));
                        g.parentNode.removeChild(g)
                    }
                } else e.push(g.textContent), g.parentNode.removeChild(g)
            }
            e = e.join("").trim();
            c = {
                is: b,
                extends: c,
                gb: d
            };
            T || Qf(a.content, b);
            ch(this);
            f = zf.test(e) || yf.test(e);
            zf.lastIndex = 0;
            yf.lastIndex = 0;
            e = df(e);
            f && U && this.a && this.a.transformRules(e, b);
            a._styleAst = e;
            a.a = d;
            d = [];
            U || (d = wg(a._styleAst));
            if (!d.length || U) e = T ? a.content : null, b = Jg[b], f = Uf(c, a._styleAst), b = f.length ? Jf(f, c.is, e, b) : void 0, a.ra = b;
            a.Ka = d
        }
    };

    function dh(a) {
        !a.b && window.ShadyCSS && window.ShadyCSS.CustomStyleInterface && (a.b = window.ShadyCSS.CustomStyleInterface, a.b.transformCallback = function(b) {
            a.ya(b)
        }, a.b.validateCallback = function() {
            requestAnimationFrame(function() {
                (a.b.enqueued || a.s) && a.I()
            })
        })
    }

    function ch(a) {
        !a.a && window.ShadyCSS && window.ShadyCSS.ApplyShim && (a.a = window.ShadyCSS.ApplyShim, a.a.invalidCallback = Vg);
        dh(a)
    }
    p.I = function() {
        ch(this);
        if (this.b) {
            var a = this.b.processStyles();
            if (this.b.enqueued) {
                if (U)
                    for (var b = 0; b < a.length; b++) {
                        var c = this.b.getStyleForCustomStyle(a[b]);
                        if (c && U && this.a) {
                            var d = Hf(c);
                            ch(this);
                            this.a.transformRules(d);
                            c.textContent = Ff(d)
                        }
                    } else
                        for (eh(this, this.c, this.i), b = 0; b < a.length; b++)(c = this.b.getStyleForCustomStyle(a[b])) && Hg(c, this.i.F);
                this.b.enqueued = !1;
                this.s && !U && this.styleDocument()
            }
        }
    };
    p.styleElement = function(a, b) {
        var c = Of(a).is,
            d = W(a);
        if (!d) {
            var e = Of(a);
            d = e.is;
            e = e.R;
            var f = Jg[d];
            d = Tg[d];
            if (d) {
                var h = d._styleAst;
                var g = d.Ka
            }
            d = sg(a, new rg(h, f, g, e))
        }
        a !== this.c && (this.s = !0);
        b && (d.K = d.K || {}, Object.assign(d.K, b));
        if (U) {
            if (d.K) {
                b = d.K;
                for (var k in b) null === k ? a.style.removeProperty(k) : a.style.setProperty(k, b[k])
            }
            if (((k = Tg[c]) || a === this.c) && k && k.ra && !Wg(k)) {
                if (Wg(k) || k._applyShimValidatingVersion !== k._applyShimNextVersion) ch(this), this.a && this.a.transformRules(k._styleAst, c), k.ra.textContent =
                    Uf(a, d.B), Xg(k);
                T && (c = a.shadowRoot) && (c.querySelector("style").textContent = Uf(a, d.B));
                d.B = k._styleAst
            }
        } else if (eh(this, a, d), d.ma && d.ma.length) {
            c = d;
            k = Of(a).is;
            d = (b = bh.fetch(k, c.F, c.ma)) ? b.styleElement : null;
            h = c.u;
            (g = b && b.u) || (g = this.N[k] = (this.N[k] || 0) + 1, g = k + "-" + g);
            c.u = g;
            g = c.u;
            e = Ig;
            e = d ? d.textContent || "" : Eg(e, a, c.F, g);
            f = W(a);
            var l = f.a;
            l && !T && l !== d && (l._useCount--, 0 >= l._useCount && l.parentNode && l.parentNode.removeChild(l));
            T ? f.a ? (f.a.textContent = e, d = f.a) : e && (d = Jf(e, g, a.shadowRoot, f.b)) : d ? d.parentNode ||
                (ug && -1 < e.indexOf("@media") && (d.textContent = e), Kf(d, null, f.b)) : e && (d = Jf(e, g, null, f.b));
            d && (d._useCount = d._useCount || 0, f.a != d && d._useCount++, f.a = d);
            g = d;
            T || (d = c.u, f = e = a.getAttribute("class") || "", h && (f = e.replace(new RegExp("\\s*x-scope\\s*" + h + "\\s*", "g"), " ")), f += (f ? " " : "") + "x-scope " + d, e !== f && Nf(a, f));
            b || bh.store(k, c.F, g, c.u)
        }
    };

    function fh(a, b) {
        return (b = b.getRootNode().host) ? W(b) ? b : fh(a, b) : a.c
    }

    function eh(a, b, c) {
        a = fh(a, b);
        var d = W(a);
        a = Object.create(d.F || null);
        var e = Dg(b, c.B);
        b = Bg(d.B, b).A;
        Object.assign(a, e.Ra, b, e.Ya);
        b = c.K;
        for (var f in b)
            if ((e = b[f]) || 0 === e) a[f] = e;
        f = Ig;
        b = Object.getOwnPropertyNames(a);
        for (e = 0; e < b.length; e++) d = b[e], a[d] = zg(f, a[d], a);
        c.F = a
    }
    p.styleDocument = function(a) {
        this.styleSubtree(this.c, a)
    };
    p.styleSubtree = function(a, b) {
        var c = a.shadowRoot;
        (c || a === this.c) && this.styleElement(a, b);
        if (b = c && (c.children || c.childNodes))
            for (a = 0; a < b.length; a++) this.styleSubtree(b[a]);
        else if (a = a.children || a.childNodes)
            for (b = 0; b < a.length; b++) this.styleSubtree(a[b])
    };
    p.ya = function(a) {
        var b = this,
            c = Hf(a);
        Gf(c, function(a) {
            if (T) og(a);
            else {
                var c = V;
                a.selector = a.parsedSelector;
                og(a);
                a.selector = a.o = Yf(c, a, c.c, void 0, void 0)
            }
            U && (ch(b), b.a && b.a.transformRule(a))
        });
        U ? a.textContent = Ff(c) : this.i.B.rules.push(c)
    };
    p.getComputedStyleValue = function(a, b) {
        var c;
        U || (c = (W(a) || W(fh(this, a))).F[b]);
        return (c = c || window.getComputedStyle(a).getPropertyValue(b)) ? c.trim() : ""
    };
    p.$a = function(a, b) {
        var c = a.getRootNode();
        b = b ? b.split(/\s/) : [];
        c = c.host && c.host.localName;
        if (!c) {
            var d = a.getAttribute("class");
            if (d) {
                d = d.split(/\s/);
                for (var e = 0; e < d.length; e++)
                    if (d[e] === V.a) {
                        c = d[e + 1];
                        break
                    }
            }
        }
        c && b.push(V.a, c);
        U || (c = W(a)) && c.u && b.push(Ig.a, c.u);
        Nf(a, b.join(" "))
    };
    p.Ma = function(a) {
        return W(a)
    };
    X.prototype.flush = X.prototype.Ba;
    X.prototype.prepareTemplate = X.prototype.prepareTemplate;
    X.prototype.styleElement = X.prototype.styleElement;
    X.prototype.styleDocument = X.prototype.styleDocument;
    X.prototype.styleSubtree = X.prototype.styleSubtree;
    X.prototype.getComputedStyleValue = X.prototype.getComputedStyleValue;
    X.prototype.setElementClass = X.prototype.$a;
    X.prototype._styleInfoForNode = X.prototype.Ma;
    X.prototype.transformCustomStyleForDocument = X.prototype.ya;
    X.prototype.getStyleAst = X.prototype.Pa;
    X.prototype.styleAstToString = X.prototype.ab;
    X.prototype.flushCustomStyles = X.prototype.I;
    Object.defineProperties(X.prototype, {
        nativeShadow: {
            get: function() {
                return T
            }
        },
        nativeCss: {
            get: function() {
                return U
            }
        }
    });
    var Y = new X,
        gh, hh;
    window.ShadyCSS && (gh = window.ShadyCSS.ApplyShim, hh = window.ShadyCSS.CustomStyleInterface);
    window.ShadyCSS = {
        ScopingShim: Y,
        prepareTemplate: function(a, b, c) {
            Y.I();
            Y.prepareTemplate(a, b, c)
        },
        styleSubtree: function(a, b) {
            Y.I();
            Y.styleSubtree(a, b)
        },
        styleElement: function(a) {
            Y.I();
            Y.styleElement(a)
        },
        styleDocument: function(a) {
            Y.I();
            Y.styleDocument(a)
        },
        getComputedStyleValue: function(a, b) {
            return Y.getComputedStyleValue(a, b)
        },
        nativeCss: U,
        nativeShadow: T
    };
    gh && (window.ShadyCSS.ApplyShim = gh);
    hh && (window.ShadyCSS.CustomStyleInterface = hh);
    var ih = window.customElements,
        jh = window.HTMLImports,
        kh = window.HTMLTemplateElement;
    window.WebComponents = window.WebComponents || {};
    if (ih && ih.polyfillWrapFlushCallback) {
        var lh, mh = function() {
                if (lh) {
                    kh.M && kh.M(window.document);
                    var a = lh;
                    lh = null;
                    a();
                    return !0
                }
            },
            nh = jh.whenReady;
        ih.polyfillWrapFlushCallback(function(a) {
            lh = a;
            nh(mh)
        });
        jh.whenReady = function(a) {
            nh(function() {
                mh() ? jh.whenReady(a) : a()
            })
        }
    }
    jh.whenReady(function() {
        requestAnimationFrame(function() {
            window.WebComponents.ready = !0;
            document.dispatchEvent(new CustomEvent("WebComponentsReady", {
                bubbles: !0
            }))
        })
    });
    var oh = document.createElement("style");
    oh.textContent = "body {transition: opacity ease-in 0.2s; } \nbody[unresolved] {opacity: 0; display: block; overflow: hidden; position: relative; } \n";
    var ph = document.querySelector("head");
    ph.insertBefore(oh, ph.firstChild);
}).call(this);
