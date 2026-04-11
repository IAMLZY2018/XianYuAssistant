import{d as X,y as Y,r as f,q as Z,s as ee,c as v,i as t,f as a,e as i,b as _,v as g,w as te,n as M,t as l,F as oe,j as se,g as z,h as ne,o as c,u as U,A as ae,B as ie,m as u,C as le,k as ce,E as w,_ as re}from"./index-3ySjUv-b.js";import{g as de,s as ue,a as pe}from"./websocket-BgfBx1MB.js";import{s as B,b as I}from"./index-qoAv1d38.js";import{M as ve,a as ke,Q as ge}from"./QRUpdateDialog-CcmQJqpT.js";import"./request-mGe2sy9-.js";import"./index-qFyJwsfU.js";import"./qrlogin-8UP3eZkc.js";const me={class:"connection-detail-page"},fe={class:"detail-header"},ye={class:"detail-content"},_e={key:0,class:"connection-main-card"},we={class:"main-card-header"},xe={class:"header-left"},Ce={class:"icon-large"},he={class:"header-info"},Te={class:"main-subtitle"},be={class:"header-right"},Se={class:"details-grid"},Me={class:"detail-section cookie-section"},ze={class:"section-header"},De={class:"section-body"},Ue={class:"info-box"},Be={class:"info-box-value cookie-value"},Ie={key:0,class:"info-box-meta"},Ve={class:"section-actions"},Ee={class:"detail-section token-section"},Ne={class:"section-header"},Le={class:"section-body"},Re={class:"info-box"},Fe={class:"info-box-value time-value"},Ge={class:"info-box"},He={class:"info-box-value token-value"},Qe={key:0,class:"info-box-meta"},We={class:"section-actions"},qe={class:"main-actions"},Ae={class:"action-wrapper"},$e={class:"action-buttons"},je={class:"logs-section"},Je={class:"logs-container"},Ke={class:"log-time"},Oe={class:"log-message"},Pe={key:0,class:"log-empty"},Xe=X({__name:"ConnectionDetail",setup(Ye){const V=le(),E=ce(),m=Y(()=>Number(V.params.id)),s=f(null),k=f(!1),y=f([]);let x=null;const C=f(!1),h=f(!1),T=f(!1),p=async(o=!1)=>{o||(k.value=!0);try{const e=await de(m.value);if(e.code===0||e.code===200)s.value=e.data,o||n("状态已更新");else throw new Error(e.msg||"获取连接状态失败")}catch(e){o||(console.error("加载连接状态失败:",e),n("加载状态失败: "+e.message,!0))}finally{k.value=!1}},N=async()=>{k.value=!0,n("正在启动连接...");try{const o=await ue(m.value);if(o.code===0||o.code===200)B("连接启动成功"),n("连接启动成功"),await p();else if(o.code===1001&&o.data?.needCaptcha)n("⚠️ 检测到需要滑块验证",!0),await w.confirm(`检测到账号需要完成滑块验证才能启动连接。

📋 操作步骤：

1️⃣ 点击下方"访问闲鱼IM"按钮，打开闲鱼消息页面

2️⃣ 在闲鱼页面完成滑块验证

3️⃣ 验证成功后，点击本页面 Cookie 和 Token 区域的"❓ 如何获取？"按钮

4️⃣ 按照帮助教程获取 Cookie 和 Token

5️⃣ 点击"✏️ 手动更新"按钮，粘贴 Cookie 和 Token

6️⃣ 更新完成后，重新点击"启动连接"即可

💡 提示：帮助按钮中有详细的图文教程，非常简单！`,"🔐 需要滑块验证",{confirmButtonText:"🌐 访问闲鱼IM",cancelButtonText:"取消",type:"warning",distinguishCancelAndClose:!0,customClass:"captcha-guide-dialog"}),window.open("https://www.goofish.com/im","_blank"),n("✅ 已打开闲鱼IM页面"),n('📌 完成验证后，请点击"❓ 如何获取？"按钮查看教程'),I("请在闲鱼IM页面完成验证，然后使用帮助按钮获取Cookie和Token");else throw new Error(o.msg||"启动连接失败")}catch(o){o!=="cancel"&&o!=="close"&&(console.error("启动连接失败:",o),n("启动连接失败: "+o.message,!0))}finally{k.value=!1}},L=async()=>{try{await w.confirm("断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？","确认断开连接",{confirmButtonText:"确定断开",cancelButtonText:"取消",type:"warning"})}catch{return}k.value=!0,n("正在断开连接...");try{const o=await pe(m.value);if(o.code===0||o.code===200)B("连接已断开"),n("连接已断开"),await p();else throw new Error(o.msg||"断开连接失败")}catch(o){console.error("断开连接失败:",o),n("断开连接失败: "+o.message,!0)}finally{k.value=!1}},R=()=>{p(),I("状态已刷新")},F=()=>{E.back()},n=(o,e=!1)=>{const d=new Date().toLocaleTimeString();y.value.push({time:d,message:o,isError:e}),y.value.length>50&&y.value.shift()},G=o=>o==null?"未知":{1:"有效",2:"过期",3:"失效"}[o]||"未知",H=o=>o==null?"info":{1:"success",2:"warning",3:"danger"}[o]||"info",Q=o=>o?new Date(o).toLocaleString("zh-CN",{year:"numeric",month:"2-digit",day:"2-digit",hour:"2-digit",minute:"2-digit",second:"2-digit"}):"未设置",D=o=>o?Date.now()>o:!1,W=o=>o?D(o)?"已过期":"有效":"未设置",q=o=>o?D(o)?"danger":"success":"info",A=()=>{w({title:"如何获取Cookie",message:`
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
    `,dangerouslyUseHTMLString:!0,confirmButtonText:"知道了",customClass:"cookie-help-dialog"})},$=()=>{w({title:"如何获取WebSocket Token",message:`
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
    `,dangerouslyUseHTMLString:!0,confirmButtonText:"知道了",customClass:"token-help-dialog"})},j=async()=>{n("Cookie已手动更新"),await p()},J=async()=>{n("Token已手动更新"),await p()},K=async()=>{n("Cookie和Token已通过扫码更新"),await p()};return Z(async()=>{await p(),x=window.setInterval(()=>{p(!0)},5e3)}),ee(()=>{x&&clearInterval(x)}),(o,e)=>{const b=z("el-icon"),d=z("el-button"),S=z("el-tag"),O=ne("loading");return c(),v("div",me,[t("div",fe,[a(d,{class:"back-btn",onClick:F,circle:""},{default:i(()=>[a(b,null,{default:i(()=>[a(U(ae))]),_:1})]),_:1}),e[6]||(e[6]=t("div",{class:"header-title"},"连接详情",-1)),s.value?(c(),_(d,{key:0,size:"small",onClick:R,circle:""},{default:i(()=>[a(b,null,{default:i(()=>[a(U(ie))]),_:1})]),_:1})):g("",!0)]),te((c(),v("div",ye,[s.value?(c(),v("div",_e,[t("div",we,[t("div",xe,[t("div",{class:M(["icon-wrapper-large",s.value.connected?"icon-success":"icon-danger"])},[t("span",Ce,l(s.value.connected?"✓":"✕"),1)],2),t("div",he,[e[7]||(e[7]=t("h2",{class:"main-title"},"连接状态",-1)),t("p",Te,"账号 ID: "+l(s.value.xianyuAccountId)+" · "+l(s.value.status),1),t("p",{class:M(["main-note",s.value.connected?"note-success":"note-danger"])},l(s.value.connected?"已连接到闲鱼服务器":"当前未连接到闲鱼服务器，无法监听消息以及执行自动化流程"),3)])]),t("div",be,[a(S,{type:s.value.connected?"success":"danger",size:"large",effect:"dark",round:"",class:"status-tag-large"},{default:i(()=>[u(l(s.value.connected?"● 已连接":"● 未连接"),1)]),_:1},8,["type"])])]),t("div",Se,[t("div",Me,[t("div",ze,[e[8]||(e[8]=t("div",{class:"section-icon"},"🍪",-1)),e[9]||(e[9]=t("div",{class:"section-title-group"},[t("h3",{class:"section-title"},"Cookie 凭证"),t("p",{class:"section-note"},"用于识别账号，如果过期无法使用任何功能")],-1)),a(S,{type:H(s.value.cookieStatus),size:"small",round:""},{default:i(()=>[u(l(G(s.value.cookieStatus)),1)]),_:1},8,["type"])]),t("div",De,[t("div",Ue,[e[10]||(e[10]=t("div",{class:"info-box-label"},"Cookie 内容",-1)),t("div",Be,l(s.value.cookieText||"未获取到Cookie"),1),s.value.cookieText?(c(),v("div",Ie," 长度: "+l(s.value.cookieText.length)+" 字符 ",1)):g("",!0)]),t("div",Ve,[a(d,{type:"primary",size:"small",onClick:e[0]||(e[0]=r=>C.value=!0),class:"manual-update-btn"},{default:i(()=>[...e[11]||(e[11]=[u(" ✏️ 手动更新 ",-1)])]),_:1}),a(d,{type:"info",size:"small",onClick:A},{default:i(()=>[...e[12]||(e[12]=[u(" ❓ 如何获取？ ",-1)])]),_:1})])])]),t("div",Ee,[t("div",Ne,[e[13]||(e[13]=t("div",{class:"section-icon"},"🔑",-1)),e[14]||(e[14]=t("div",{class:"section-title-group"},[t("h3",{class:"section-title"},"WebSocket Token"),t("p",{class:"section-note"},"这个是收取消息的凭证，如果异常，可能是账号被锁人机验证，需要隔段时间再试一试")],-1)),a(S,{type:q(s.value.tokenExpireTime),size:"small",round:""},{default:i(()=>[u(l(W(s.value.tokenExpireTime)),1)]),_:1},8,["type"])]),t("div",Le,[t("div",Re,[e[15]||(e[15]=t("div",{class:"info-box-label"},"⏰ 过期时间",-1)),t("div",Fe,l(Q(s.value.tokenExpireTime)),1)]),t("div",Ge,[e[16]||(e[16]=t("div",{class:"info-box-label"},"Token 内容",-1)),t("div",He,l(s.value.websocketToken||"未获取到Token"),1),s.value.websocketToken?(c(),v("div",Qe," 长度: "+l(s.value.websocketToken.length)+" 字符 ",1)):g("",!0)]),t("div",We,[a(d,{type:"primary",size:"small",onClick:e[1]||(e[1]=r=>h.value=!0),class:"manual-update-btn"},{default:i(()=>[...e[17]||(e[17]=[u(" ✏️ 手动更新 ",-1)])]),_:1}),a(d,{type:"info",size:"small",onClick:$},{default:i(()=>[...e[18]||(e[18]=[u(" ❓ 如何获取？ ",-1)])]),_:1})])])])]),t("div",qe,[t("div",Ae,[t("div",$e,[s.value.connected?(c(),_(d,{key:0,type:"danger",size:"default",onClick:L,class:"main-action-btn"},{default:i(()=>[...e[19]||(e[19]=[u(" ⏸ 断开连接 ",-1)])]),_:1})):(c(),_(d,{key:1,type:"success",size:"default",onClick:N,class:"main-action-btn start-connection-btn"},{default:i(()=>[...e[20]||(e[20]=[u(" ▶ 启动连接 ",-1)])]),_:1})),a(d,{type:"primary",size:"default",onClick:e[2]||(e[2]=r=>T.value=!0),class:"main-action-btn qr-update-btn"},{default:i(()=>[...e[21]||(e[21]=[u(" 📱 扫码更新 ",-1)])]),_:1})]),e[22]||(e[22]=t("div",{class:"action-tip"}," ⚠️ 请勿频繁启用连接和断开连接，否则容易触发滑动窗口人机校验，导致账号暂时不可用 ",-1)),e[23]||(e[23]=t("div",{class:"action-tip qr-update-tip"}," 💡 扫码更新：通过扫码登录完成更新Cookie与Token ",-1))])])])):g("",!0),t("div",je,[e[24]||(e[24]=t("div",{class:"logs-header"},"操作日志",-1)),t("div",Je,[(c(!0),v(oe,null,se(y.value,(r,P)=>(c(),v("div",{key:P,class:M(["log-entry",{"log-error":r.isError}])},[t("span",Ke,"["+l(r.time)+"]",1),t("span",Oe,l(r.message),1)],2))),128)),y.value.length===0?(c(),v("div",Pe," 暂无日志记录 ")):g("",!0)])])])),[[O,k.value]]),s.value?(c(),_(ve,{key:0,modelValue:C.value,"onUpdate:modelValue":e[3]||(e[3]=r=>C.value=r),"account-id":m.value,"current-cookie":s.value.cookieText||"",onSuccess:j},null,8,["modelValue","account-id","current-cookie"])):g("",!0),s.value?(c(),_(ke,{key:1,modelValue:h.value,"onUpdate:modelValue":e[4]||(e[4]=r=>h.value=r),"account-id":m.value,"current-token":s.value.websocketToken||"",onSuccess:J},null,8,["modelValue","account-id","current-token"])):g("",!0),a(ge,{modelValue:T.value,"onUpdate:modelValue":e[5]||(e[5]=r=>T.value=r),"account-id":m.value,onSuccess:K},null,8,["modelValue","account-id"])])}}}),it=re(Xe,[["__scopeId","data-v-8eea0b0a"]]);export{it as default};
