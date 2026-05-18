export function showConfirm(message: string, title: string = '确认'): Promise<void> {
  return new Promise((resolve, reject) => {
    const overlay = document.createElement('div')
    overlay.style.cssText = `
      position:fixed;inset:0;z-index:99998;
      background:rgba(0,0,0,0.20);
      backdrop-filter:blur(28px) saturate(1.8);-webkit-backdrop-filter:blur(28px) saturate(1.8);
      display:flex;align-items:center;justify-content:center;
      animation:confirm-in .2s ease forwards;
    `
    const dialog = document.createElement('div')
    dialog.style.cssText = `
      background:rgba(255,255,255,0.72);
      backdrop-filter:blur(40px) saturate(2);-webkit-backdrop-filter:blur(40px) saturate(2);
      border:1px solid rgba(255,255,255,0.75);
      border-radius:20px;padding:0;width:320px;max-width:90vw;
      box-shadow:0 16px 48px rgba(0,0,0,0.16),0 2px 8px rgba(0,0,0,0.08);
      overflow:hidden;
      font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
    `
    dialog.innerHTML = `
      <div style="padding:20px 20px 4px;text-align:center;">
        <div style="font-size:17px;font-weight:600;color:#1c1c1e;margin-bottom:8px;">${title}</div>
        <div style="font-size:14px;color:rgba(28,28,30,.72);line-height:1.5;">${message}</div>
      </div>
      <div style="display:flex;border-top:0.5px solid rgba(60,60,67,.12);margin-top:16px;">
        <button id="confirm-cancel" style="flex:1;border:none;background:transparent;font-size:16px;font-weight:500;color:rgba(28,28,30,.55);cursor:pointer;padding:12px 0;-webkit-tap-highlight-color:transparent;font-family:inherit;">取消</button>
        <button id="confirm-ok" style="flex:1;border:none;border-left:0.5px solid rgba(60,60,67,.12);background:transparent;font-size:16px;font-weight:600;color:#0A84FF;cursor:pointer;padding:12px 0;-webkit-tap-highlight-color:transparent;font-family:inherit;">确定</button>
      </div>
    `
    overlay.appendChild(dialog)
    document.body.appendChild(overlay)

    const cleanup = () => {
      overlay.style.animation = 'confirm-out .15s ease forwards'
      setTimeout(() => overlay.remove(), 150)
    }

    dialog.querySelector('#confirm-cancel')!.addEventListener('click', () => {
      cleanup()
      reject('cancel')
    })
    dialog.querySelector('#confirm-ok')!.addEventListener('click', () => {
      cleanup()
      resolve()
    })
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        cleanup()
        reject('cancel')
      }
    })
  })
}
