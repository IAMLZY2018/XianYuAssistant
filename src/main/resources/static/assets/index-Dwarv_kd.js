import{g as Me}from"./account-Cw_6Ct6B.js";import{s as pe,a as ge,g as fe}from"./websocket-BZ1csKyp.js";import{s as te,b as oe}from"./index-B5hIWSuo.js";import{M as ke,a as me,Q as ye}from"./QRUpdateDialog-Dy1qu1gA.js";import{d as _e,r as d,y as he,x as Ie,q as we,z as Ue,b as T,e as c,n as D,g as V,o as i,w as Z,c as r,i as e,t as a,f as v,m as f,v as m,F,j as ee,E,h as xe,_ as be,s as Ve,u as De}from"./index-DYkH9IuM.js";import"./request-BlHqG2ZS.js";import"./index-qFyJwsfU.js";import"./qrlogin-BaWa9RXl.js";const Ee={class:"connection-detail"},Be={key:0,class:"detail-content"},Le={class:"main-card-header"},Ae={class:"header-left"},Ne={class:"icon-large"},Re={class:"header-info"},We={class:"main-subtitle"},He={class:"header-right"},Ge={class:"details-grid"},Fe={class:"detail-section cookie-section"},Qe={class:"section-header"},qe={class:"section-body"},je={class:"info-box"},Pe={class:"info-box-value cookie-value"},Je={key:0,class:"info-box-meta"},Ke={class:"section-actions"},Oe={class:"detail-section token-section"},Xe={class:"section-header"},Ye={class:"section-body"},Ze={class:"info-box"},et={class:"info-box-value time-value"},tt={class:"info-box"},ot={class:"info-box-value token-value"},st={key:0,class:"info-box-meta"},nt={class:"section-actions"},lt={class:"main-actions"},it={class:"action-wrapper"},at={class:"action-buttons"},ct={class:"logs-section"},dt={class:"logs-container"},rt={class:"log-time"},ut={class:"log-message"},vt={key:0,class:"log-empty"},pt=_e({__name:"ConnectionDetailDialog",props:{modelValue:{type:Boolean},accountId:{}},emits:["update:modelValue","refresh"],setup(N,{emit:$}){const p=N,g=$,n=d(null),y=d(!1),C=d([]);let w=null;const M=d(!1),B=d(!1),L=d(!1),R=d(!1),A=()=>{R.value=window.innerWidth<768},X=he(()=>R.value?"95%":"750px"),S=async(l=!1)=>{if(p.accountId){l||(y.value=!0);try{const o=await fe(p.accountId);if(o.code===0||o.code===200)n.value=o.data,l||k("状态已更新");else throw new Error(o.msg||"获取连接状态失败")}catch(o){l||(console.error("加载连接状态失败:",o),k("加载状态失败: "+o.message,!0))}finally{y.value=!1}}},Q=async()=>{if(p.accountId){y.value=!0,k("正在启动连接...");try{const l=await pe(p.accountId);if(l.code===0||l.code===200)te("连接启动成功"),k("连接启动成功"),await S();else if(l.code===1001&&l.data?.needCaptcha)k("⚠️ 检测到需要滑块验证",!0),await E.confirm(`检测到账号需要完成滑块验证才能启动连接。

📋 操作步骤：

1️⃣ 点击下方"访问闲鱼IM"按钮，打开闲鱼消息页面

2️⃣ 在闲鱼页面完成滑块验证

3️⃣ 验证成功后，点击本页面 Cookie 和 Token 区域的"❓ 如何获取？"按钮

4️⃣ 按照帮助教程获取 Cookie 和 Token

5️⃣ 点击"✏️ 手动更新"按钮，粘贴 Cookie 和 Token

6️⃣ 更新完成后，重新点击"启动连接"即可

💡 提示：帮助按钮中有详细的图文教程，非常简单！`,"🔐 需要滑块验证",{confirmButtonText:"🌐 访问闲鱼IM",cancelButtonText:"取消",type:"warning",distinguishCancelAndClose:!0,customClass:"captcha-guide-dialog"}),window.open("https://www.goofish.com/im","_blank"),k("✅ 已打开闲鱼IM页面"),k('📌 完成验证后，请点击"❓ 如何获取？"按钮查看教程'),oe("请在闲鱼IM页面完成验证，然后使用帮助按钮获取Cookie和Token");else throw new Error(l.msg||"启动连接失败")}catch(l){l!=="cancel"&&l!=="close"&&(console.error("启动连接失败:",l),k("启动连接失败: "+l.message,!0))}finally{y.value=!1}}},W=async()=>{if(p.accountId){try{await E.confirm("断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？","确认断开连接",{confirmButtonText:"确定断开",cancelButtonText:"取消",type:"warning"})}catch{return}y.value=!0,k("正在断开连接...");try{const l=await ge(p.accountId);if(l.code===0||l.code===200)te("连接已断开"),k("连接已断开"),await S();else throw new Error(l.msg||"断开连接失败")}catch(l){console.error("断开连接失败:",l),k("断开连接失败: "+l.message,!0)}finally{y.value=!1}}},q=()=>{S(),oe("状态已刷新")},k=(l,o=!1)=>{const x=new Date().toLocaleTimeString();C.value.push({time:x,message:l,isError:o}),C.value.length>50&&C.value.shift()},j=l=>l==null?"未知":{1:"有效",2:"过期",3:"失效"}[l]||"未知",I=l=>l==null?"info":{1:"success",2:"warning",3:"danger"}[l]||"info",P=l=>l?new Date(l).toLocaleString("zh-CN",{year:"numeric",month:"2-digit",day:"2-digit",hour:"2-digit",minute:"2-digit",second:"2-digit"}):"未设置",Y=l=>l?Date.now()>l:!1,J=l=>l?Y(l)?"已过期":"有效":"未设置",z=l=>l?Y(l)?"danger":"success":"info",se=()=>{E({title:"如何获取Cookie",message:`
      <div style="text-align: left;">
        <p style="margin-bottom: 12px;">请按照以下步骤获取Cookie：</p>
        <ol style="margin-left: 20px; line-height: 1.8;">
          <li>打开浏览器，访问闲鱼网站并登录</li>
          <li>按F12打开开发者工具</li>
          <li>切换到"网络"(Network)标签</li>
          <li>刷新页面</li>
          <li>在请求列表中找到任意请求</li>
          <li>在请求头中找到Cookie字段</li>
          <li>复制完整的Cookie值</li>
        </ol>
        <div style="margin-top: 16px; text-align: center;">
          <img 
            src="/cookieGet.png" 
            class="cookie-help-image"
            alt="Cookie获取示例" 
            onerror="this.style.display='none'"
            onclick="window.open('/cookieGet.png', '_blank')"
            title="点击查看大图"
          />
        </div>
        <p style="margin-top: 12px; color: #909399; font-size: 12px; text-align: center;">
          💡 点击图片可查看大图
        </p>
        <p style="margin-top: 8px; color: #f56c6c; font-size: 12px; text-align: center;">
          ⚠️ Cookie包含敏感信息，请勿泄露给他人
        </p>
      </div>
    `,dangerouslyUseHTMLString:!0,confirmButtonText:"知道了",customClass:"cookie-help-dialog"})},ne=()=>{E({title:"如何获取WebSocket Token",message:`
      <div style="text-align: left;">
        <p style="margin-bottom: 12px;">请按照以下步骤获取WebSocket Token：</p>
        <ol style="margin-left: 20px; line-height: 1.8;">
          <li>打开浏览器，访问 <a href="https://www.goofish.com/im" target="_blank" style="color: #409eff;">闲鱼IM页面</a> 并登录</li>
          <li>按F12打开开发者工具</li>
          <li>切换到"网络"(Network)标签</li>
          <li>在页面中进行任意操作（如点击聊天）</li>
          <li>在请求列表中找到WebSocket连接请求</li>
          <li>查看请求参数或响应中的Token信息</li>
          <li>复制完整的Token值</li>
        </ol>
        <div style="margin-top: 16px; text-align: center;">
          <img 
            src="/tokenGet.png" 
            class="token-help-image"
            alt="Token获取示例" 
            onerror="this.style.display='none'"
            onclick="window.open('/tokenGet.png', '_blank')"
            title="点击查看大图"
          />
        </div>
        <p style="margin-top: 12px; color: #909399; font-size: 12px; text-align: center;">
          💡 点击图片可查看大图
        </p>
        <p style="margin-top: 8px; color: #f56c6c; font-size: 12px; text-align: center;">
          ⚠️ Token包含敏感信息，请勿泄露给他人
        </p>
      </div>
    `,dangerouslyUseHTMLString:!0,confirmButtonText:"知道了",customClass:"token-help-dialog"})},le=async()=>{k("Cookie已手动更新"),await S()},_=async()=>{k("Token已手动更新"),await S()},K=async()=>{k("Cookie和Token已通过扫码更新"),await S()},O=()=>{g("update:modelValue",!1),w&&(clearInterval(w),w=null)};return Ie(()=>p.modelValue,l=>{l?(S(),w=window.setInterval(()=>{S(!0)},5e3)):w&&(clearInterval(w),w=null)}),we(()=>{A(),window.addEventListener("resize",A)}),Ue(()=>{window.removeEventListener("resize",A),w&&clearInterval(w)}),(l,o)=>{const H=V("el-tag"),x=V("el-button"),ie=V("el-dialog"),ae=xe("loading");return i(),T(ie,{"model-value":N.modelValue,title:"连接详情",width:X.value,onClose:O,class:D(["connection-detail-dialog",{"mobile-dialog":R.value}]),"close-on-click-modal":!1,"close-on-press-escape":!1},{footer:c(()=>[v(x,{onClick:O},{default:c(()=>[...o[24]||(o[24]=[f("关闭",-1)])]),_:1}),v(x,{type:"primary",onClick:q,loading:y.value},{default:c(()=>[...o[25]||(o[25]=[f("刷新状态",-1)])]),_:1},8,["loading"])]),default:c(()=>[Z((i(),r("div",Ee,[n.value?(i(),r("div",Be,[e("div",Le,[e("div",Ae,[e("div",{class:D(["icon-wrapper-large",n.value.connected?"icon-success":"icon-danger"])},[e("span",Ne,a(n.value.connected?"✓":"✕"),1)],2),e("div",Re,[o[6]||(o[6]=e("h2",{class:"main-title"},"连接状态",-1)),e("p",We,"账号 ID: "+a(n.value.xianyuAccountId)+" · "+a(n.value.status),1),e("p",{class:D(["main-note",n.value.connected?"note-success":"note-danger"])},a(n.value.connected?"已连接到闲鱼服务器":"当前未连接到闲鱼服务器，无法监听消息以及执行自动化流程"),3)])]),e("div",He,[v(H,{type:n.value.connected?"success":"danger",size:"large",effect:"dark",round:"",class:"status-tag-large"},{default:c(()=>[f(a(n.value.connected?"● 已连接":"● 未连接"),1)]),_:1},8,["type"])])]),e("div",Ge,[e("div",Fe,[e("div",Qe,[o[7]||(o[7]=e("div",{class:"section-icon"},"🍪",-1)),o[8]||(o[8]=e("div",{class:"section-title-group"},[e("h3",{class:"section-title"},"Cookie 凭证"),e("p",{class:"section-note"},"用于识别账号，如果过期无法使用任何功能")],-1)),v(H,{type:I(n.value.cookieStatus),size:"small",round:""},{default:c(()=>[f(a(j(n.value.cookieStatus)),1)]),_:1},8,["type"])]),e("div",qe,[e("div",je,[o[9]||(o[9]=e("div",{class:"info-box-label"},"Cookie 内容",-1)),e("div",Pe,a(n.value.cookieText||"未获取到Cookie"),1),n.value.cookieText?(i(),r("div",Je," 长度: "+a(n.value.cookieText.length)+" 字符 ",1)):m("",!0)]),e("div",Ke,[v(x,{type:"primary",size:"small",onClick:o[0]||(o[0]=b=>M.value=!0),class:"manual-update-btn"},{default:c(()=>[...o[10]||(o[10]=[f(" ✏️ 手动更新 ",-1)])]),_:1}),v(x,{type:"info",size:"small",onClick:se},{default:c(()=>[...o[11]||(o[11]=[f(" ❓ 如何获取？ ",-1)])]),_:1})])])]),e("div",Oe,[e("div",Xe,[o[12]||(o[12]=e("div",{class:"section-icon"},"🔑",-1)),o[13]||(o[13]=e("div",{class:"section-title-group"},[e("h3",{class:"section-title"},"WebSocket Token"),e("p",{class:"section-note"},"这个是收取消息的凭证，如果异常，可能是账号被锁人机验证，需要隔段时间再试一试")],-1)),v(H,{type:z(n.value.tokenExpireTime),size:"small",round:""},{default:c(()=>[f(a(J(n.value.tokenExpireTime)),1)]),_:1},8,["type"])]),e("div",Ye,[e("div",Ze,[o[14]||(o[14]=e("div",{class:"info-box-label"},"⏰ 过期时间",-1)),e("div",et,a(P(n.value.tokenExpireTime)),1)]),e("div",tt,[o[15]||(o[15]=e("div",{class:"info-box-label"},"Token 内容",-1)),e("div",ot,a(n.value.websocketToken||"未获取到Token"),1),n.value.websocketToken?(i(),r("div",st," 长度: "+a(n.value.websocketToken.length)+" 字符 ",1)):m("",!0)]),e("div",nt,[v(x,{type:"primary",size:"small",onClick:o[1]||(o[1]=b=>B.value=!0),class:"manual-update-btn"},{default:c(()=>[...o[16]||(o[16]=[f(" ✏️ 手动更新 ",-1)])]),_:1}),v(x,{type:"info",size:"small",onClick:ne},{default:c(()=>[...o[17]||(o[17]=[f(" ❓ 如何获取？ ",-1)])]),_:1})])])])]),e("div",lt,[e("div",it,[e("div",at,[n.value.connected?(i(),T(x,{key:0,type:"danger",size:"default",onClick:W,class:"main-action-btn"},{default:c(()=>[...o[18]||(o[18]=[f(" ⏸ 断开连接 ",-1)])]),_:1})):(i(),T(x,{key:1,type:"success",size:"default",onClick:Q,class:"main-action-btn start-connection-btn"},{default:c(()=>[...o[19]||(o[19]=[f(" ▶ 启动连接 ",-1)])]),_:1})),v(x,{type:"primary",size:"default",onClick:o[2]||(o[2]=b=>L.value=!0),class:"main-action-btn qr-update-btn"},{default:c(()=>[...o[20]||(o[20]=[f(" 📱 扫码更新 ",-1)])]),_:1})]),o[21]||(o[21]=e("div",{class:"action-tip"}," ⚠️ 请勿频繁启用连接和断开连接，否则容易触发滑动窗口人机校验，导致账号暂时不可用 ",-1)),o[22]||(o[22]=e("div",{class:"action-tip qr-update-tip"}," 💡 扫码更新：通过扫码登录完成更新Cookie与Token ",-1))])]),e("div",ct,[o[23]||(o[23]=e("div",{class:"logs-header"},"操作日志",-1)),e("div",dt,[(i(!0),r(F,null,ee(C.value,(b,ce)=>(i(),r("div",{key:ce,class:D(["log-entry",{"log-error":b.isError}])},[e("span",rt,"["+a(b.time)+"]",1),e("span",ut,a(b.message),1)],2))),128)),C.value.length===0?(i(),r("div",vt," 暂无日志记录 ")):m("",!0)])])])):m("",!0)])),[[ae,y.value]]),n.value?(i(),T(ke,{key:0,modelValue:M.value,"onUpdate:modelValue":o[3]||(o[3]=b=>M.value=b),"account-id":N.accountId||0,"current-cookie":n.value.cookieText||"",onSuccess:le},null,8,["modelValue","account-id","current-cookie"])):m("",!0),n.value?(i(),T(me,{key:1,modelValue:B.value,"onUpdate:modelValue":o[4]||(o[4]=b=>B.value=b),"account-id":N.accountId||0,"current-token":n.value.websocketToken||"",onSuccess:_},null,8,["modelValue","account-id","current-token"])):m("",!0),v(ye,{modelValue:L.value,"onUpdate:modelValue":o[5]||(o[5]=b=>L.value=b),"account-id":N.accountId||0,onSuccess:K},null,8,["modelValue","account-id"])]),_:1},8,["model-value","width","class"])}}}),gt=be(pt,[["__scopeId","data-v-29f438fa"]]),ft={class:"connection-page"},kt={class:"connection-container"},mt=["onClick"],yt={class:"account-avatar"},_t={class:"account-info"},ht={class:"account-name"},wt={class:"account-id"},xt={key:0,class:"loading-more"},bt={key:1,class:"no-more"},Tt={class:"panel-header"},Ct={key:0,class:"empty-state"},St={key:1,class:"status-content"},zt={key:0,class:"connection-main-card"},$t={class:"main-card-header"},Mt={class:"header-left"},It={class:"icon-large"},Ut={class:"header-info"},Vt={class:"main-subtitle"},Dt={class:"header-right"},Et={class:"details-grid"},Bt={class:"detail-section cookie-section"},Lt={class:"section-header"},At={class:"section-body"},Nt={class:"info-box"},Rt={class:"info-box-value cookie-value"},Wt={key:0,class:"info-box-meta"},Ht={class:"section-actions"},Gt={class:"detail-section token-section"},Ft={class:"section-header"},Qt={class:"section-body"},qt={class:"info-box"},jt={class:"info-box-value time-value"},Pt={class:"info-box"},Jt={class:"info-box-value token-value"},Kt={key:0,class:"info-box-meta"},Ot={class:"section-actions"},Xt={class:"main-actions"},Yt={class:"action-wrapper"},Zt={class:"action-buttons"},eo={class:"logs-section"},to={class:"logs-container"},oo={class:"log-time"},so={class:"log-message"},no={key:0,class:"log-empty"},lo={class:"mobile-page-header"},io={class:"mobile-page-subtitle"},ao={class:"mobile-accounts-list"},co=["onClick"],ro={class:"mobile-account-avatar"},uo={class:"mobile-account-info"},vo={class:"mobile-account-name"},po={class:"mobile-account-id"},go=_e({__name:"index",setup(N){const $=d(!1),p=d([]),g=d(null),n=d(null),y=d(!1);d(!1);const C=d([]);let w=null;const M=d(!1),B=()=>{M.value=window.innerWidth<768},L=d(!1),R=d(null),A=d(null),X=d(1);d(20);const S=d(0),Q=d(!0),W=d(!1),q=d(!1),k=d(!1),j=d(!1),I=he(()=>p.value.find(s=>s.id===g.value)),P=async(s=!1)=>{s?W.value=!0:($.value=!0,X.value=1,p.value=[]);try{const t=await Me();if(t.code===0||t.code===200){const h=t.data?.accounts||[];s?p.value=[...p.value,...h]:p.value=h,S.value=t.data?.total||h.length,Q.value=p.value.length<S.value}else throw new Error(t.msg||"获取账号列表失败")}catch(t){console.error("加载账号列表失败:",t),s||(p.value=[])}finally{$.value=!1,W.value=!1}},Y=()=>{if(!A.value||W.value||!Q.value)return;const{scrollTop:s,scrollHeight:t,clientHeight:h}=A.value;t-s-h<100&&(X.value++,P(!0))},J=s=>{if(M.value){R.value=s,L.value=!0;return}g.value=s,z(s),w&&clearInterval(w),w=window.setInterval(()=>{g.value&&z(g.value,!0)},5e3)},z=async(s,t=!1)=>{t||(y.value=!0);try{const h=await fe(s);if(h.code===0||h.code===200)n.value=h.data,t||_("状态已更新");else throw new Error(h.msg||"获取连接状态失败")}catch(h){t||(console.error("加载连接状态失败:",h),_("加载状态失败: "+h.message,!0))}finally{y.value=!1}},se=async()=>{if(g.value){y.value=!0,_("正在启动连接...");try{const s=await pe(g.value);if(s.code===0||s.code===200)te("连接启动成功"),_("连接启动成功"),await z(g.value);else if(s.code===1001&&s.data?.needCaptcha)_("⚠️ 检测到需要滑块验证",!0),await E.confirm(`检测到账号需要完成滑块验证才能启动连接。

📋 操作步骤：

1️⃣ 点击下方"访问闲鱼IM"按钮，打开闲鱼消息页面

2️⃣ 在闲鱼页面完成滑块验证

3️⃣ 验证成功后，点击本页面 Cookie 和 Token 区域的"❓ 如何获取？"按钮

4️⃣ 按照帮助教程获取 Cookie 和 Token

5️⃣ 点击"✏️ 手动更新"按钮，粘贴 Cookie 和 Token

6️⃣ 更新完成后，重新点击"启动连接"即可

💡 提示：帮助按钮中有详细的图文教程，非常简单！`,"🔐 需要滑块验证",{confirmButtonText:"🌐 访问闲鱼IM",cancelButtonText:"取消",type:"warning",distinguishCancelAndClose:!0,customClass:"captcha-guide-dialog"}),window.open("https://www.goofish.com/im","_blank"),_("✅ 已打开闲鱼IM页面"),_('📌 完成验证后，请点击"❓ 如何获取？"按钮查看教程'),oe("请在闲鱼IM页面完成验证，然后使用帮助按钮获取Cookie和Token");else throw new Error(s.msg||"启动连接失败")}catch(s){s!=="cancel"&&s!=="close"&&(console.error("启动连接失败:",s),_("启动连接失败: "+s.message,!0))}finally{y.value=!1}}},ne=async()=>{if(g.value){try{await E.confirm("断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？","确认断开连接",{confirmButtonText:"确定断开",cancelButtonText:"取消",type:"warning"})}catch{return}y.value=!0,_("正在断开连接...");try{const s=await ge(g.value);if(s.code===0||s.code===200)te("连接已断开"),_("连接已断开"),await z(g.value);else throw new Error(s.msg||"断开连接失败")}catch(s){console.error("断开连接失败:",s),_("断开连接失败: "+s.message,!0)}finally{y.value=!1}}},le=()=>{g.value&&(z(g.value),oe("状态已刷新"))},_=(s,t=!1)=>{const G=new Date().toLocaleTimeString();C.value.push({time:G,message:s,isError:t}),C.value.length>50&&C.value.shift()},K=s=>s.accountNote||s.unb||"未命名账号",O=s=>K(s).charAt(0),l=s=>s==null?"未知":{1:"有效",2:"过期",3:"失效"}[s]||"未知",o=s=>s==null?"info":{1:"success",2:"warning",3:"danger"}[s]||"info",H=s=>s?new Date(s).toLocaleString("zh-CN",{year:"numeric",month:"2-digit",day:"2-digit",hour:"2-digit",minute:"2-digit",second:"2-digit"}):"未设置",x=s=>s?Date.now()>s:!1,ie=s=>s?x(s)?"已过期":"有效":"未设置",ae=s=>s?x(s)?"danger":"success":"info",b=()=>{q.value=!0},ce=()=>{k.value=!0},Te=()=>{E({title:"如何获取Cookie",message:`
      <div style="text-align: left;">
        <p style="margin-bottom: 12px;">请按照以下步骤获取Cookie：</p>
        <ol style="margin-left: 20px; line-height: 1.8;">
          <li>打开浏览器，访问闲鱼网站并登录</li>
          <li>按F12打开开发者工具</li>
          <li>切换到"网络"(Network)标签</li>
          <li>刷新页面</li>
          <li>在请求列表中找到任意请求</li>
          <li>在请求头中找到Cookie字段</li>
          <li>复制完整的Cookie值</li>
        </ol>
        <div style="margin-top: 16px; text-align: center;">
          <img 
            src="/cookieGet.png" 
            class="cookie-help-image"
            alt="Cookie获取示例" 
            onerror="this.style.display='none'"
            onclick="window.open('/cookieGet.png', '_blank')"
            title="点击查看大图"
          />
        </div>
        <p style="margin-top: 12px; color: #909399; font-size: 12px; text-align: center;">
          💡 点击图片可查看大图
        </p>
        <p style="margin-top: 8px; color: #f56c6c; font-size: 12px; text-align: center;">
          ⚠️ Cookie包含敏感信息，请勿泄露给他人
        </p>
      </div>
    `,dangerouslyUseHTMLString:!0,confirmButtonText:"知道了",customClass:"cookie-help-dialog"})},Ce=()=>{E({title:"如何获取WebSocket Token",message:`
      <div style="text-align: left;">
        <p style="margin-bottom: 12px;">请按照以下步骤获取WebSocket Token：</p>
        <ol style="margin-left: 20px; line-height: 1.8;">
          <li>打开浏览器，访问 <a href="https://www.goofish.com/im" target="_blank" style="color: #409eff;">闲鱼IM页面</a> 并登录</li>
          <li>按F12打开开发者工具</li>
          <li>切换到"网络"(Network)标签</li>
          <li>在页面中进行任意操作（如点击聊天）</li>
          <li>在请求列表中找到WebSocket连接请求</li>
          <li>查看请求参数或响应中的Token信息</li>
          <li>复制完整的Token值</li>
        </ol>
        <div style="margin-top: 16px; text-align: center;">
          <img 
            src="/tokenGet.png" 
            class="token-help-image"
            alt="Token获取示例" 
            onerror="this.style.display='none'"
            onclick="window.open('/tokenGet.png', '_blank')"
            title="点击查看大图"
          />
        </div>
        <p style="margin-top: 12px; color: #909399; font-size: 12px; text-align: center;">
          💡 点击图片可查看大图
        </p>
        <p style="margin-top: 8px; color: #f56c6c; font-size: 12px; text-align: center;">
          ⚠️ Token包含敏感信息，请勿泄露给他人
        </p>
      </div>
    `,dangerouslyUseHTMLString:!0,confirmButtonText:"知道了",customClass:"token-help-dialog"})},Se=async()=>{_("Cookie已手动更新"),g.value&&await z(g.value)},ze=async()=>{_("Token已手动更新"),g.value&&await z(g.value)},$e=async()=>{_("Cookie和Token已通过扫码更新"),g.value&&await z(g.value)};return we(async()=>{B(),window.addEventListener("resize",B),await P(),!M.value&&p.value.length>0&&J(p.value[0]?.id||0)}),Ve(()=>{window.removeEventListener("resize",B),w&&clearInterval(w)}),(s,t)=>{const h=V("el-icon"),G=V("el-empty"),ve=V("el-card"),U=V("el-button"),de=V("el-tag"),re=xe("loading");return i(),r("div",ft,[M.value?(i(),r(F,{key:1},[e("div",lo,[t[28]||(t[28]=e("h1",{class:"mobile-page-title"},"连接管理",-1)),e("span",io,"共 "+a(p.value.length)+" 个账号",1)]),Z((i(),r("div",ao,[(i(!0),r(F,null,ee(p.value,u=>(i(),r("div",{key:u.id,class:"mobile-account-item",onClick:ue=>J(u.id)},[e("div",ro,a(O(u)),1),e("div",uo,[e("div",vo,a(K(u)),1),e("div",po,"ID: "+a(u.id),1)]),t[29]||(t[29]=e("div",{class:"mobile-account-arrow"},"›",-1))],8,co))),128)),!$.value&&p.value.length===0?(i(),T(G,{key:0,description:"暂无账号数据","image-size":80})):m("",!0)])),[[re,$.value]])],64)):(i(),r(F,{key:0},[t[27]||(t[27]=e("div",{class:"page-header"},[e("h1",{class:"page-title"},"连接管理")],-1)),e("div",kt,[v(ve,{class:"account-panel"},{header:c(()=>[...t[5]||(t[5]=[e("div",{class:"panel-header"},[e("span",{class:"panel-title"},"闲鱼账号")],-1)])]),default:c(()=>[Z((i(),r("div",{class:"account-list",ref_key:"accountListRef",ref:A,onScroll:Y},[(i(!0),r(F,null,ee(p.value,u=>(i(),r("div",{key:u.id,class:D(["account-item",{active:g.value===u.id}]),onClick:ue=>J(u.id)},[e("div",yt,a(O(u)),1),e("div",_t,[e("div",ht,a(K(u)),1),e("div",wt,"ID: "+a(u.id),1)])],10,mt))),128)),W.value?(i(),r("div",xt,[v(h,{class:"is-loading"},{default:c(()=>[v(De($))]),_:1}),t[6]||(t[6]=e("span",null,"加载中...",-1))])):m("",!0),!Q.value&&p.value.length>0?(i(),r("div",bt," 已加载全部 "+a(p.value.length)+" 个账号 ",1)):m("",!0),!$.value&&p.value.length===0?(i(),T(G,{key:2,description:"暂无账号数据","image-size":80})):m("",!0)],32)),[[re,$.value]])]),_:1}),v(ve,{class:"status-panel"},{header:c(()=>[e("div",Tt,[t[7]||(t[7]=e("span",{class:"panel-title"},"连接状态",-1)),g.value?(i(),T(U,{key:0,size:"small",icon:"Refresh",onClick:le,circle:""})):m("",!0)])]),default:c(()=>[g.value?Z((i(),r("div",St,[n.value?(i(),r("div",zt,[e("div",$t,[e("div",Mt,[e("div",{class:D(["icon-wrapper-large",n.value.connected?"icon-success":"icon-danger"])},[e("span",It,a(n.value.connected?"✓":"✕"),1)],2),e("div",Ut,[t[9]||(t[9]=e("h2",{class:"main-title"},"连接状态",-1)),e("p",Vt,"账号 ID: "+a(n.value.xianyuAccountId)+" · "+a(n.value.status),1),e("p",{class:D(["main-note",n.value.connected?"note-success":"note-danger"])},a(n.value.connected?"已连接到闲鱼服务器":"当前未连接到闲鱼服务器，无法监听消息以及执行自动化流程"),3)])]),e("div",Dt,[v(de,{type:n.value.connected?"success":"danger",size:"large",effect:"dark",round:"",class:"status-tag-large"},{default:c(()=>[f(a(n.value.connected?"● 已连接":"● 未连接"),1)]),_:1},8,["type"])])]),e("div",Et,[e("div",Bt,[e("div",Lt,[t[10]||(t[10]=e("div",{class:"section-icon"},"🍪",-1)),t[11]||(t[11]=e("div",{class:"section-title-group"},[e("h3",{class:"section-title"},"Cookie 凭证"),e("p",{class:"section-note"},"用于识别账号，如果过期无法使用任何功能")],-1)),v(de,{type:o(n.value.cookieStatus),size:"small",round:""},{default:c(()=>[f(a(l(n.value.cookieStatus)),1)]),_:1},8,["type"])]),e("div",At,[e("div",Nt,[t[12]||(t[12]=e("div",{class:"info-box-label"},"Cookie 内容",-1)),e("div",Rt,a(n.value.cookieText||"未获取到Cookie"),1),n.value.cookieText?(i(),r("div",Wt," 长度: "+a(n.value.cookieText.length)+" 字符 ",1)):m("",!0)]),e("div",Ht,[v(U,{type:"primary",size:"small",onClick:b,class:"manual-update-btn"},{default:c(()=>[...t[13]||(t[13]=[f(" ✏️ 手动更新 ",-1)])]),_:1}),v(U,{type:"info",size:"small",onClick:Te},{default:c(()=>[...t[14]||(t[14]=[f(" ❓ 如何获取？ ",-1)])]),_:1})])])]),e("div",Gt,[e("div",Ft,[t[15]||(t[15]=e("div",{class:"section-icon"},"🔑",-1)),t[16]||(t[16]=e("div",{class:"section-title-group"},[e("h3",{class:"section-title"},"WebSocket Token"),e("p",{class:"section-note"},"这个是收取消息的凭证，如果异常，可能是账号被锁人机验证，需要隔段时间再试一试")],-1)),v(de,{type:ae(n.value.tokenExpireTime),size:"small",round:""},{default:c(()=>[f(a(ie(n.value.tokenExpireTime)),1)]),_:1},8,["type"])]),e("div",Qt,[e("div",qt,[t[17]||(t[17]=e("div",{class:"info-box-label"},"⏰ 过期时间",-1)),e("div",jt,a(H(n.value.tokenExpireTime)),1)]),e("div",Pt,[t[18]||(t[18]=e("div",{class:"info-box-label"},"Token 内容",-1)),e("div",Jt,a(n.value.websocketToken||"未获取到Token"),1),n.value.websocketToken?(i(),r("div",Kt," 长度: "+a(n.value.websocketToken.length)+" 字符 ",1)):m("",!0)]),e("div",Ot,[v(U,{type:"primary",size:"small",onClick:ce,class:"manual-update-btn"},{default:c(()=>[...t[19]||(t[19]=[f(" ✏️ 手动更新 ",-1)])]),_:1}),v(U,{type:"info",size:"small",onClick:Ce},{default:c(()=>[...t[20]||(t[20]=[f(" ❓ 如何获取？ ",-1)])]),_:1})])])])]),e("div",Xt,[e("div",Yt,[e("div",Zt,[n.value.connected?(i(),T(U,{key:0,type:"danger",size:"default",onClick:ne,class:"main-action-btn"},{default:c(()=>[...t[21]||(t[21]=[f(" ⏸ 断开连接 ",-1)])]),_:1})):(i(),T(U,{key:1,type:"success",size:"default",onClick:se,class:"main-action-btn start-connection-btn"},{default:c(()=>[...t[22]||(t[22]=[f(" ▶ 启动连接 ",-1)])]),_:1})),v(U,{type:"primary",size:"default",onClick:t[0]||(t[0]=u=>j.value=!0),class:"main-action-btn qr-update-btn"},{default:c(()=>[...t[23]||(t[23]=[f(" 📱 扫码更新 ",-1)])]),_:1})]),t[24]||(t[24]=e("div",{class:"action-tip"}," ⚠️ 请勿频繁启用连接和断开连接，否则容易触发滑动窗口人机校验，导致账号暂时不可用 ",-1)),t[25]||(t[25]=e("div",{class:"action-tip qr-update-tip"}," 💡 扫码更新：通过扫码登录完成更新Cookie与Token ",-1))])])])):m("",!0),e("div",eo,[t[26]||(t[26]=e("div",{class:"logs-header"},"操作日志",-1)),e("div",to,[(i(!0),r(F,null,ee(C.value,(u,ue)=>(i(),r("div",{key:ue,class:D(["log-entry",{"log-error":u.isError}])},[e("span",oo,"["+a(u.time)+"]",1),e("span",so,a(u.message),1)],2))),128)),C.value.length===0?(i(),r("div",no," 暂无日志记录 ")):m("",!0)])])])),[[re,y.value]]):(i(),r("div",Ct,[v(G,{description:"请选择一个账号查看连接状态","image-size":100},{image:c(()=>[...t[8]||(t[8]=[e("div",{class:"empty-icon"},"🔗",-1)])]),_:1})]))]),_:1})])],64)),I.value&&n.value?(i(),T(ke,{key:2,modelValue:q.value,"onUpdate:modelValue":t[1]||(t[1]=u=>q.value=u),"account-id":I.value.id,"current-cookie":n.value.cookieText||"",onSuccess:Se},null,8,["modelValue","account-id","current-cookie"])):m("",!0),I.value&&n.value?(i(),T(me,{key:3,modelValue:k.value,"onUpdate:modelValue":t[2]||(t[2]=u=>k.value=u),"account-id":I.value.id,"current-token":n.value.websocketToken||"",onSuccess:ze},null,8,["modelValue","account-id","current-token"])):m("",!0),I.value?(i(),T(ye,{key:4,modelValue:j.value,"onUpdate:modelValue":t[3]||(t[3]=u=>j.value=u),"account-id":I.value.id,onSuccess:$e},null,8,["modelValue","account-id"])):m("",!0),v(gt,{modelValue:L.value,"onUpdate:modelValue":t[4]||(t[4]=u=>L.value=u),"account-id":R.value,onRefresh:P},null,8,["modelValue","account-id"])])}}}),bo=be(go,[["__scopeId","data-v-bc6fe33a"]]);export{bo as default};
