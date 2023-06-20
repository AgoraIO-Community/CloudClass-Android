var ht=Object.defineProperty,lt=Object.defineProperties;var ft=Object.getOwnPropertyDescriptors;var ee=Object.getOwnPropertySymbols;var Te=Object.prototype.hasOwnProperty,ze=Object.prototype.propertyIsEnumerable;var ce=(g,m,y)=>m in g?ht(g,m,{enumerable:!0,configurable:!0,writable:!0,value:y}):g[m]=y,Oe=(g,m)=>{for(var y in m||(m={}))Te.call(m,y)&&ce(g,y,m[y]);if(ee)for(var y of ee(m))ze.call(m,y)&&ce(g,y,m[y]);return g},$e=(g,m)=>lt(g,ft(m));var _e=(g,m)=>{var y={};for(var R in g)Te.call(g,R)&&m.indexOf(R)<0&&(y[R]=g[R]);if(g!=null&&ee)for(var R of ee(g))m.indexOf(R)<0&&ze.call(g,R)&&(y[R]=g[R]);return y};var te=(g,m,y)=>(ce(g,typeof m!="symbol"?m+"":m,y),y);var NetlessAppTalkative=function(g){"use strict";const m="!#%()*+,-./:;=?@[]^_`{|}~ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",y=m.length,R=Array(20),W=()=>{for(let e=0;e<20;e++)R[e]=m.charAt(Math.random()*y);return R.join("")};class J{constructor(){this.disposers=new Map}add(t,r=W()){return this.flush(r),this.disposers.set(r,t()),r}addDisposer(t,r=W()){return this.flush(r),this.disposers.set(r,t),r}addEventListener(t,r,i,o,u=W()){return this.add(()=>(t.addEventListener(r,i,o),()=>t.removeEventListener(r,i,o)),u),u}setTimeout(t,r,i=W()){return this.add(()=>{const o=window.setTimeout(()=>{this.remove(i),t()},r);return()=>window.clearTimeout(o)},i)}setInterval(t,r,i=W()){return this.add(()=>{const o=window.setInterval(t,r);return()=>window.clearInterval(o)},i)}remove(t){const r=this.disposers.get(t);return this.disposers.delete(t),r}flush(t){const r=this.remove(t);if(r)try{r()}catch(i){console.error(i)}}flushAll(){this.disposers.forEach(t=>{try{t()}catch(r){console.error(r)}}),this.disposers.clear()}}var Me=typeof globalThis!="undefined"?globalThis:typeof window!="undefined"?window:typeof global!="undefined"?global:typeof self!="undefined"?self:{},re={exports:{}};(function(e,t){(function(r,i){{var o=i();e&&e.exports&&(t=e.exports=o),t.randomColor=o}})(Me,function(){var r=null,i={};Y();var o=[],u=function(n){if(n=n||{},n.seed!==void 0&&n.seed!==null&&n.seed===parseInt(n.seed,10))r=n.seed;else if(typeof n.seed=="string")r=Z(n.seed);else{if(n.seed!==void 0&&n.seed!==null)throw new TypeError("The seed value must be an integer or string");r=null}var a,s,c;if(n.count!==null&&n.count!==void 0){for(var d=n.count,f=[],b=0;b<n.count;b++)o.push(!1);for(n.count=null;d>f.length;){var M=u(n);r!==null&&(n.seed=r),f.push(M)}return n.count=d,f}return a=l(n),s=h(a,n),c=p(a,s,n),v([a,s,c],n)};function l(n){if(o.length>0){var a=oe(n.hue),s=x(a),c=(a[1]-a[0])/o.length,d=parseInt((s-a[0])/c);o[d]===!0?d=(d+2)%o.length:o[d]=!0;var f=(a[0]+d*c)%359,b=(a[0]+(d+1)*c)%359;return a=[f,b],s=x(a),s<0&&(s=360+s),s}else{var a=S(n.hue);return s=x(a),s<0&&(s=360+s),s}}function h(n,a){if(a.hue==="monochrome")return 0;if(a.luminosity==="random")return x([0,100]);var s=D(n),c=s[0],d=s[1];switch(a.luminosity){case"bright":c=55;break;case"dark":c=d-10;break;case"light":d=55;break}return x([c,d])}function p(n,a,s){var c=T(n,a),d=100;switch(s.luminosity){case"dark":d=c+20;break;case"light":c=(d+c)/2;break;case"random":c=0,d=100;break}return x([c,d])}function v(n,a){switch(a.format){case"hsvArray":return n;case"hslArray":return L(n);case"hsl":var s=L(n);return"hsl("+s[0]+", "+s[1]+"%, "+s[2]+"%)";case"hsla":var c=L(n),b=a.alpha||Math.random();return"hsla("+c[0]+", "+c[1]+"%, "+c[2]+"%, "+b+")";case"rgbArray":return O(n);case"rgb":var d=O(n);return"rgb("+d.join(", ")+")";case"rgba":var f=O(n),b=a.alpha||Math.random();return"rgba("+f.join(", ")+", "+b+")";default:return X(n)}}function T(n,a){for(var s=w(n).lowerBounds,c=0;c<s.length-1;c++){var d=s[c][0],f=s[c][1],b=s[c+1][0],M=s[c+1][1];if(a>=d&&a<=b){var P=(M-f)/(b-d),z=f-P*d;return P*a+z}}return 0}function S(n){if(typeof parseInt(n)=="number"){var a=parseInt(n);if(a<360&&a>0)return[a,a]}if(typeof n=="string"){if(i[n]){var s=i[n];if(s.hueRange)return s.hueRange}else if(n.match(/^#?([0-9A-F]{3}|[0-9A-F]{6})$/i)){var c=I(n)[0];return[c,c]}}return[0,360]}function D(n){return w(n).saturationRange}function w(n){n>=334&&n<=360&&(n-=360);for(var a in i){var s=i[a];if(s.hueRange&&n>=s.hueRange[0]&&n<=s.hueRange[1])return i[a]}return"Color not found"}function x(n){if(r===null){var a=.618033988749895,s=Math.random();return s+=a,s%=1,Math.floor(n[0]+s*(n[1]+1-n[0]))}else{var c=n[1]||1,d=n[0]||0;r=(r*9301+49297)%233280;var f=r/233280;return Math.floor(d+f*(c-d))}}function X(n){var a=O(n);function s(d){var f=d.toString(16);return f.length==1?"0"+f:f}var c="#"+s(a[0])+s(a[1])+s(a[2]);return c}function E(n,a,s){var c=s[0][0],d=s[s.length-1][0],f=s[s.length-1][1],b=s[0][1];i[n]={hueRange:a,lowerBounds:s,saturationRange:[c,d],brightnessRange:[f,b]}}function Y(){E("monochrome",null,[[0,0],[100,0]]),E("red",[-26,18],[[20,100],[30,92],[40,89],[50,85],[60,78],[70,70],[80,60],[90,55],[100,50]]),E("orange",[18,46],[[20,100],[30,93],[40,88],[50,86],[60,85],[70,70],[100,70]]),E("yellow",[46,62],[[25,100],[40,94],[50,89],[60,86],[70,84],[80,82],[90,80],[100,75]]),E("green",[62,178],[[30,100],[40,90],[50,85],[60,81],[70,74],[80,64],[90,50],[100,40]]),E("blue",[178,257],[[20,100],[30,86],[40,80],[50,74],[60,60],[70,52],[80,44],[90,39],[100,35]]),E("purple",[257,282],[[20,100],[30,87],[40,79],[50,70],[60,65],[70,59],[80,52],[90,45],[100,42]]),E("pink",[282,334],[[20,100],[30,90],[40,86],[60,84],[80,80],[90,75],[100,73]])}function O(n){var a=n[0];a===0&&(a=1),a===360&&(a=359),a=a/360;var s=n[1]/100,c=n[2]/100,d=Math.floor(a*6),f=a*6-d,b=c*(1-s),M=c*(1-f*s),P=c*(1-(1-f)*s),z=256,B=256,C=256;switch(d){case 0:z=c,B=P,C=b;break;case 1:z=M,B=c,C=b;break;case 2:z=b,B=c,C=P;break;case 3:z=b,B=M,C=c;break;case 4:z=P,B=b,C=c;break;case 5:z=c,B=b,C=M;break}var ut=[Math.floor(z*255),Math.floor(B*255),Math.floor(C*255)];return ut}function I(n){n=n.replace(/^#/,""),n=n.length===3?n.replace(/(.)/g,"$1$1"):n;var a=parseInt(n.substr(0,2),16)/255,s=parseInt(n.substr(2,2),16)/255,c=parseInt(n.substr(4,2),16)/255,d=Math.max(a,s,c),f=d-Math.min(a,s,c),b=d?f/d:0;switch(d){case a:return[60*((s-c)/f%6)||0,b,d];case s:return[60*((c-a)/f+2)||0,b,d];case c:return[60*((a-s)/f+4)||0,b,d]}}function L(n){var a=n[0],s=n[1]/100,c=n[2]/100,d=(2-s)*c;return[a,Math.round(s*c/(d<1?d:2-d)*1e4)/100,d/2*100]}function Z(n){for(var a=0,s=0;s!==n.length&&!(a>=Number.MAX_SAFE_INTEGER);s++)a+=n.charCodeAt(s);return a}function oe(n){if(isNaN(n)){if(typeof n=="string"){if(i[n]){var s=i[n];if(s.hueRange)return s.hueRange}else if(n.match(/^#?([0-9A-F]{3}|[0-9A-F]{6})$/i)){var c=I(n)[0];return w(c).hueRange}}}else{var a=parseInt(n);if(a<360&&a>0)return w(n).hueRange}return[0,360]}return u})})(re,re.exports);var Be=re.exports;class Ce{constructor(t="NetlessApp",r="error"){te(this,"kind");te(this,"debug");te(this,"color",Be({luminosity:"dark"}));this.kind=t,this.debug=r}log(...t){if(this.debug===!0||this.debug==="log")return this._log("log",t)}warn(...t){if(this.debug&&this.debug!=="error")return this._log("warn",t)}error(...t){if(this.debug)return this._log("error",t)}_log(t,r){console[t](`%c[${this.kind}]:`,`color: ${this.color}; font-weight: bold;`,...r)}}function Ae(e){var h;const t=e.getRoom(),r=e.getDisplayer(),i=r.observerId,o=(h=r.state.roomMembers.find(p=>p.memberId===i))==null?void 0:h.payload,u=(t==null?void 0:t.uid)||(o==null?void 0:o.uid)||"",l=(o==null?void 0:o.nickName)||u;return{memberId:i,uid:u,nickName:l}}function Ne(e){const t=e.indexOf("?",1);return t!==-1?{search:e.slice(t),pathname:e.slice(0,t)}:{search:"",pathname:e}}function De(e,t){const{pathname:r,search:i}=Ne(e);return r+(i?`${i}&`:"?")+t}function A(e){return document.createElement(e)}function F(e,t,r){r==null?e.removeAttribute(t):e.setAttribute(t,r)}function q(e,t){return e.appendChild(t)}function de(e){var t;return(t=e.parentNode)==null?void 0:t.removeChild(e)}const ue=Promise.resolve();function G(e){const t=[];return{get value(){return e},set(r){e=r,t.forEach(i=>i(e))},subscribe(r){return t.push(r),ue.then(()=>r(e)),()=>{t.splice(t.indexOf(r),1)}}}}var $=[],Ie=function(){return $.some(function(e){return e.activeTargets.length>0})},Le=function(){return $.some(function(e){return e.skippedTargets.length>0})},he="ResizeObserver loop completed with undelivered notifications.",Pe=function(){var e;typeof ErrorEvent=="function"?e=new ErrorEvent("error",{message:he}):(e=document.createEvent("Event"),e.initEvent("error",!1,!1),e.message=he),window.dispatchEvent(e)},H;(function(e){e.BORDER_BOX="border-box",e.CONTENT_BOX="content-box",e.DEVICE_PIXEL_CONTENT_BOX="device-pixel-content-box"})(H||(H={}));var _=function(e){return Object.freeze(e)},We=function(){function e(t,r){this.inlineSize=t,this.blockSize=r,_(this)}return e}(),le=function(){function e(t,r,i,o){return this.x=t,this.y=r,this.width=i,this.height=o,this.top=this.y,this.left=this.x,this.bottom=this.top+this.height,this.right=this.left+this.width,_(this)}return e.prototype.toJSON=function(){var t=this,r=t.x,i=t.y,o=t.top,u=t.right,l=t.bottom,h=t.left,p=t.width,v=t.height;return{x:r,y:i,top:o,right:u,bottom:l,left:h,width:p,height:v}},e.fromRect=function(t){return new e(t.x,t.y,t.width,t.height)},e}(),ne=function(e){return e instanceof SVGElement&&"getBBox"in e},fe=function(e){if(ne(e)){var t=e.getBBox(),r=t.width,i=t.height;return!r&&!i}var o=e,u=o.offsetWidth,l=o.offsetHeight;return!(u||l||e.getClientRects().length)},ve=function(e){var t,r;if(e instanceof Element)return!0;var i=(r=(t=e)===null||t===void 0?void 0:t.ownerDocument)===null||r===void 0?void 0:r.defaultView;return!!(i&&e instanceof i.Element)},Fe=function(e){switch(e.tagName){case"INPUT":if(e.type!=="image")break;case"VIDEO":case"AUDIO":case"EMBED":case"OBJECT":case"CANVAS":case"IFRAME":case"IMG":return!0}return!1},V=typeof window!="undefined"?window:{},j=new WeakMap,ge=/auto|scroll/,He=/^tb|vertical/,Ve=/msie|trident/i.test(V.navigator&&V.navigator.userAgent),k=function(e){return parseFloat(e||"0")},N=function(e,t,r){return e===void 0&&(e=0),t===void 0&&(t=0),r===void 0&&(r=!1),new We((r?t:e)||0,(r?e:t)||0)},pe=_({devicePixelContentBoxSize:N(),borderBoxSize:N(),contentBoxSize:N(),contentRect:new le(0,0,0,0)}),be=function(e,t){if(t===void 0&&(t=!1),j.has(e)&&!t)return j.get(e);if(fe(e))return j.set(e,pe),pe;var r=getComputedStyle(e),i=ne(e)&&e.ownerSVGElement&&e.getBBox(),o=!Ve&&r.boxSizing==="border-box",u=He.test(r.writingMode||""),l=!i&&ge.test(r.overflowY||""),h=!i&&ge.test(r.overflowX||""),p=i?0:k(r.paddingTop),v=i?0:k(r.paddingRight),T=i?0:k(r.paddingBottom),S=i?0:k(r.paddingLeft),D=i?0:k(r.borderTopWidth),w=i?0:k(r.borderRightWidth),x=i?0:k(r.borderBottomWidth),X=i?0:k(r.borderLeftWidth),E=S+v,Y=p+T,O=X+w,I=D+x,L=h?e.offsetHeight-I-e.clientHeight:0,Z=l?e.offsetWidth-O-e.clientWidth:0,oe=o?E+O:0,n=o?Y+I:0,a=i?i.width:k(r.width)-oe-Z,s=i?i.height:k(r.height)-n-L,c=a+E+Z+O,d=s+Y+L+I,f=_({devicePixelContentBoxSize:N(Math.round(a*devicePixelRatio),Math.round(s*devicePixelRatio),u),borderBoxSize:N(c,d,u),contentBoxSize:N(a,s,u),contentRect:new le(S,p,a,s)});return j.set(e,f),f},me=function(e,t,r){var i=be(e,r),o=i.borderBoxSize,u=i.contentBoxSize,l=i.devicePixelContentBoxSize;switch(t){case H.DEVICE_PIXEL_CONTENT_BOX:return l;case H.BORDER_BOX:return o;default:return u}},Xe=function(){function e(t){var r=be(t);this.target=t,this.contentRect=r.contentRect,this.borderBoxSize=_([r.borderBoxSize]),this.contentBoxSize=_([r.contentBoxSize]),this.devicePixelContentBoxSize=_([r.devicePixelContentBoxSize])}return e}(),we=function(e){if(fe(e))return 1/0;for(var t=0,r=e.parentNode;r;)t+=1,r=r.parentNode;return t},Je=function(){var e=1/0,t=[];$.forEach(function(l){if(l.activeTargets.length!==0){var h=[];l.activeTargets.forEach(function(v){var T=new Xe(v.target),S=we(v.target);h.push(T),v.lastReportedSize=me(v.target,v.observedBox),S<e&&(e=S)}),t.push(function(){l.callback.call(l.observer,h,l.observer)}),l.activeTargets.splice(0,l.activeTargets.length)}});for(var r=0,i=t;r<i.length;r++){var o=i[r];o()}return e},ye=function(e){$.forEach(function(r){r.activeTargets.splice(0,r.activeTargets.length),r.skippedTargets.splice(0,r.skippedTargets.length),r.observationTargets.forEach(function(o){o.isActive()&&(we(o.target)>e?r.activeTargets.push(o):r.skippedTargets.push(o))})})},qe=function(){var e=0;for(ye(e);Ie();)e=Je(),ye(e);return Le()&&Pe(),e>0},ie,xe=[],Ge=function(){return xe.splice(0).forEach(function(e){return e()})},je=function(e){if(!ie){var t=0,r=document.createTextNode(""),i={characterData:!0};new MutationObserver(function(){return Ge()}).observe(r,i),ie=function(){r.textContent=""+(t?t--:t++)}}xe.push(e),ie()},Ue=function(e){je(function(){requestAnimationFrame(e)})},U=0,Ke=function(){return!!U},Qe=250,Ye={attributes:!0,characterData:!0,childList:!0,subtree:!0},Re=["resize","load","transitionend","animationend","animationstart","animationiteration","keyup","keydown","mouseup","mousedown","mouseover","mouseout","blur","focus"],Ee=function(e){return e===void 0&&(e=0),Date.now()+e},se=!1,Ze=function(){function e(){var t=this;this.stopped=!0,this.listener=function(){return t.schedule()}}return e.prototype.run=function(t){var r=this;if(t===void 0&&(t=Qe),!se){se=!0;var i=Ee(t);Ue(function(){var o=!1;try{o=qe()}finally{if(se=!1,t=i-Ee(),!Ke())return;o?r.run(1e3):t>0?r.run(t):r.start()}})}},e.prototype.schedule=function(){this.stop(),this.run()},e.prototype.observe=function(){var t=this,r=function(){return t.observer&&t.observer.observe(document.body,Ye)};document.body?r():V.addEventListener("DOMContentLoaded",r)},e.prototype.start=function(){var t=this;this.stopped&&(this.stopped=!1,this.observer=new MutationObserver(this.listener),this.observe(),Re.forEach(function(r){return V.addEventListener(r,t.listener,!0)}))},e.prototype.stop=function(){var t=this;this.stopped||(this.observer&&this.observer.disconnect(),Re.forEach(function(r){return V.removeEventListener(r,t.listener,!0)}),this.stopped=!0)},e}(),ae=new Ze,ke=function(e){!U&&e>0&&ae.start(),U+=e,!U&&ae.stop()},et=function(e){return!ne(e)&&!Fe(e)&&getComputedStyle(e).display==="inline"},tt=function(){function e(t,r){this.target=t,this.observedBox=r||H.CONTENT_BOX,this.lastReportedSize={inlineSize:0,blockSize:0}}return e.prototype.isActive=function(){var t=me(this.target,this.observedBox,!0);return et(this.target)&&(this.lastReportedSize=t),this.lastReportedSize.inlineSize!==t.inlineSize||this.lastReportedSize.blockSize!==t.blockSize},e}(),rt=function(){function e(t,r){this.activeTargets=[],this.skippedTargets=[],this.observationTargets=[],this.observer=t,this.callback=r}return e}(),K=new WeakMap,Se=function(e,t){for(var r=0;r<e.length;r+=1)if(e[r].target===t)return r;return-1},Q=function(){function e(){}return e.connect=function(t,r){var i=new rt(t,r);K.set(t,i)},e.observe=function(t,r,i){var o=K.get(t),u=o.observationTargets.length===0;Se(o.observationTargets,r)<0&&(u&&$.push(o),o.observationTargets.push(new tt(r,i&&i.box)),ke(1),ae.schedule())},e.unobserve=function(t,r){var i=K.get(t),o=Se(i.observationTargets,r),u=i.observationTargets.length===1;o>=0&&(u&&$.splice($.indexOf(i),1),i.observationTargets.splice(o,1),ke(-1))},e.disconnect=function(t){var r=this,i=K.get(t);i.observationTargets.slice().forEach(function(o){return r.unobserve(t,o.target)}),i.activeTargets.splice(0,i.activeTargets.length)},e}(),nt=function(){function e(t){if(arguments.length===0)throw new TypeError("Failed to construct 'ResizeObserver': 1 argument required, but only 0 present.");if(typeof t!="function")throw new TypeError("Failed to construct 'ResizeObserver': The callback provided as parameter 1 is not a function.");Q.connect(this,t)}return e.prototype.observe=function(t,r){if(arguments.length===0)throw new TypeError("Failed to execute 'observe' on 'ResizeObserver': 1 argument required, but only 0 present.");if(!ve(t))throw new TypeError("Failed to execute 'observe' on 'ResizeObserver': parameter 1 is not of type 'Element");Q.observe(this,t,r)},e.prototype.unobserve=function(t){if(arguments.length===0)throw new TypeError("Failed to execute 'unobserve' on 'ResizeObserver': 1 argument required, but only 0 present.");if(!ve(t))throw new TypeError("Failed to execute 'unobserve' on 'ResizeObserver': parameter 1 is not of type 'Element");Q.unobserve(this,t)},e.prototype.disconnect=function(){Q.disconnect(this)},e.toString=function(){return"function ResizeObserver () { [polyfill code] }"},e}(),it=`.app-talkative-container{width:100%;height:100%;overflow:hidden;display:flex;justify-content:center;align-items:center;flex-direction:column}.app-talkative-container iframe{width:100%;height:100%;border:none;display:block}.app-talkative-footer{position:absolute;bottom:0;left:0;width:100%;display:flex;align-items:center;justify-content:center;gap:4px;box-sizing:border-box;background-color:#fff9;height:26px;padding:0 16px;border-top:1px solid #eeeef7;color:#191919}.telebox-color-scheme-dark .app-talkative-footer{color:#a6a6a8;background:#2d2d33;border-top:none}.app-talkative-page{font-variant-numeric:tabular-nums}.app-talkative-btn{box-sizing:border-box;width:26px;height:26px;font-size:14px;font-family:monospace;margin:0;padding:3px;border:none;border-radius:1px;outline:none;color:currentColor;background:transparent;transition:background .4s;-webkit-tap-highlight-color:rgba(0,0,0,0);cursor:pointer;user-select:none;-webkit-user-select:none}.app-talkative-btn:hover{background:rgba(237,237,240,.9)}.telebox-color-scheme-dark .app-talkative-btn:hover{background:#212126}.telebox-color-scheme-dark .app-talkative-container{color:#eee}.app-talkative-btn:disabled{opacity:0;cursor:default}
`;const st=window.ResizeObserver||nt;class at{constructor(t){this.context=t,this.sideEffect=new J,this.box=this.context.getBox(),this.role=G(2),this.ratio=G(1.7777777777777777),this.$content=A("div"),this.$iframe=A("iframe"),F(this.$content,"class","app-talkative-container"),q(this.$content,this.$iframe),this.$content.dataset.appKind="Talkative"}_on_update_role(t){this.$content.dataset.role=String(t),this.$content.classList.toggle("owner",t===0)}_on_update_ratio(t,r){const{width:i,height:o}=r?r.contentRect:this.$content.getBoundingClientRect();if(i/t>o){const u=o*t;this.$iframe.style.width=`${u}px`,this.$iframe.style.height=""}else if(i/t<o){const u=i/t;this.$iframe.style.width="",this.$iframe.style.height=`${u}px`}}_observe_content_resize(){const t=new st(r=>{this._on_update_ratio(this.ratio.value,r[0])});return t.observe(this.$content),t.disconnect.bind(t)}mount(){return this.box.mountStyles(it),this.box.mountContent(this.$content),this.sideEffect.addDisposer(this.role.subscribe(this._on_update_role.bind(this))),this.sideEffect.addDisposer(this.ratio.subscribe(this._on_update_ratio.bind(this))),this.sideEffect.addDisposer(this._observe_content_resize()),this.destroy.bind(this)}destroy(){this.sideEffect.flushAll(),de(this.$content)}postMessage(t){var r;(r=this.$iframe.contentWindow)==null||r.postMessage(t,"*")}}class ot{constructor(t,r,i){this.context=t,this.onPrev=r,this.onNext=i,this.sideEffect=new J,this.box=this.context.getBox(),this.role=G(2),this.text=G("..."),this.$footer=A("div"),this.$btnLeft=A("button"),this.$btnRight=A("button"),this.$span=A("span"),q(this.$footer,this.$btnLeft),q(this.$footer,this.$span),q(this.$footer,this.$btnRight),F(this.$footer,"class","app-talkative-footer"),F(this.$btnLeft,"class","app-talkative-btn app-talkative-btn-left"),F(this.$btnRight,"class","app-talkative-btn app-talkative-btn-right"),F(this.$span,"class","app-talkative-page"),this.$btnLeft.textContent="<",this.$btnRight.textContent=">",this.$btnLeft.addEventListener("click",this.onPrev),this.$btnRight.addEventListener("click",this.onNext)}_on_update_role(t){this.$btnLeft.disabled=t===2,this.$btnRight.disabled=t===2,this.$footer.classList.toggle("owner",t===0)}_on_update_text(t){this.$span.textContent=t}mount(){return this.box.mountFooter(this.$footer),this.sideEffect.addDisposer(this.role.subscribe(this._on_update_role.bind(this))),this.sideEffect.addDisposer(this.text.subscribe(this._on_update_text.bind(this))),this.destroy.bind(this)}destroy(){this.sideEffect.flushAll(),de(this.$footer)}}function ct(i){var o=i,{context:e,logger:t}=o,r=_e(o,["context","logger"]);const u=new J,l={onPagenum({totalPages:h}){e.getIsWritable()&&h&&e.storage.setState({pageNum:h})},onLoadComplete(h){r.onRatioChanged(h.coursewareRatio),e.getIsWritable()&&h.totalPages&&e.storage.setState({pageNum:h.totalPages});const{page:p,lastMsg:v}=e.storage.state;v&&r.postMessage(v),r.postMessage(JSON.stringify({method:"onJumpPage",toPage:p}))},onFileMessage(h){if(e.getIsWritable()){e.dispatchMagixEvent("broadcast",JSON.stringify(h));const p=JSON.stringify($e(Oe({},h),{isRestore:!0}));e.storage.setState({lastMsg:p})}}};return u.addDisposer(e.addMagixEventListener("broadcast",({payload:h})=>{r.postMessage(h)})),u.addEventListener(window,"message",h=>{if(!!r.isSentBySelf(h.source))if(typeof h.data=="string")try{const p=JSON.parse(h.data);if(typeof p=="object"&&p!==null){const v=l[p.method];v?v(p):t.warn("unknown message",p)}}catch(p){t.warn("error when parsing message",p)}else typeof h.data=="object"&&h.data!==null&&t.log("unhandled permission command",h.data)}),u.flushAll.bind(u)}const dt={kind:"Talkative",setup(e){e.storage.ensureState({src:"https://example.org",uid:"",page:1,pageNum:1,lastMsg:""});const t=(e.getAppOptions()||{}).debug,r=new Ce("Talkative",t),{uid:i,memberId:o,nickName:u}=Ae(e),l=new J;r.log("my uid",i);const h=()=>{const{page:w}=e.storage.state;e.getIsWritable()&&w>1&&e.storage.setState({page:w-1})},p=()=>{const{page:w,pageNum:x}=e.storage.state;e.getIsWritable()&&w<x&&e.storage.setState({page:w+1})},v=new at(e),T=new ot(e,h,p),S=v.postMessage.bind(v);l.addDisposer(ct({context:e,logger:r,postMessage:S,onRatioChanged:v.ratio.set.bind(v.ratio),isSentBySelf:w=>w===v.$iframe.contentWindow})),l.addDisposer(e.storage.addStateChangedListener(()=>{const w=e.storage.state.uid===i?0:2;v.role.set(w),T.role.set(w);const{page:x,pageNum:X}=e.storage.state;S(JSON.stringify({method:"onJumpPage",toPage:x})),T.text.set(`${x}/${X}`)}));const D=()=>{l.addDisposer(v.mount()),l.addDisposer(T.mount());const w=e.storage.state.uid===i?0:2,x=`userid=${o}&role=${w}&name=${u}`;v.$iframe.src=De(e.storage.state.src,x)};if(e.storage.state.uid)ue.then(D);else{const w=l.addDisposer(e.storage.addStateChangedListener(()=>{e.storage.state.uid&&(l.flush(w),D())}));e.isAddApp&&(r.log("no teacher's uid, setting myself..."),e.storage.setState({uid:i}))}e.emitter.on("destroy",()=>{r.log("destroy"),l.flushAll()})}};return g.default=dt,Object.defineProperty(g,"__esModule",{value:!0}),g[Symbol.toStringTag]="Module",g}({});
//# sourceMappingURL=main.iife.js.map