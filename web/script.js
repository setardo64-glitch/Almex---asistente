// Almex Asistente - JavaScript para la página de descarga

document.addEventListener('DOMContentLoaded', function() {
    // Actualizar fecha de última actualización
    updateLastUpdateDate();
    
    // Simular contador de descargas (en producción usarías analytics reales)
    updateDownloadCounter();
    
    // Agregar efectos de hover y animaciones
    addInteractiveEffects();
    
    // Detectar dispositivo y mostrar instrucciones específicas
    detectDevice();
});

function updateLastUpdateDate() {
    const lastUpdateElement = document.getElementById('lastUpdate');
    const today = new Date();
    const options = { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
    };
    lastUpdateElement.textContent = today.toLocaleDateString('es-ES', options);
}

function updateDownloadCounter() {
    // En producción, esto vendría de una API o analytics
    const downloadCountElement = document.getElementById('downloadCount');
    let count = localStorage.getItem('almex-download-count') || 0;
    downloadCountElement.textContent = `${count} descargas`;
}

function addInteractiveEffects() {
    // Efecto de partículas en el botón de descarga
    const downloadBtn = document.getElementById('downloadBtn');
    
    downloadBtn.addEventListener('click', function(e) {
        // Incrementar contador local
        let count = parseInt(localStorage.getItem('almex-download-count') || 0);
        count++;
        localStorage.setItem('almex-download-count', count);
        updateDownloadCounter();
        
        // Efecto visual de descarga
        createDownloadEffect(e.target);
        
        // Analytics (opcional)
        trackDownload();
    });
    
    // Animación de scroll suave
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
}

function createDownloadEffect(button) {
    // Crear efecto de "descargando"
    const originalText = button.innerHTML;
    button.innerHTML = '<span class="btn-icon">⏳</span> DESCARGANDO...';
    button.style.pointerEvents = 'none';
    
    setTimeout(() => {
        button.innerHTML = '<span class="btn-icon">✅</span> DESCARGADO';
        setTimeout(() => {
            button.innerHTML = originalText;
            button.style.pointerEvents = 'auto';
        }, 2000);
    }, 1000);
}

function detectDevice() {
    const userAgent = navigator.userAgent.toLowerCase();
    const isAndroid = userAgent.includes('android');
    const isIOS = userAgent.includes('iphone') || userAgent.includes('ipad');
    
    if (!isAndroid && !isIOS) {
        // Mostrar aviso para usuarios de escritorio
        showDesktopNotice();
    } else if (isIOS) {
        // Mostrar aviso para usuarios de iOS
        showIOSNotice();
    }
}

function showDesktopNotice() {
    const notice = document.createElement('div');
    notice.className = 'desktop-notice';
    notice.innerHTML = `
        <div class="notice-content">
            <span class="notice-icon">💻</span>
            <p>Estás visitando desde una computadora. Esta app es solo para Android.</p>
            <button onclick="this.parentElement.parentElement.remove()">Entendido</button>
        </div>
    `;
    
    notice.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: var(--bg-secondary);
        border: 1px solid var(--accent-blue);
        border-radius: 12px;
        padding: 1rem;
        max-width: 300px;
        z-index: 1000;
        animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(notice);
    
    // Auto-remove después de 10 segundos
    setTimeout(() => {
        if (notice.parentElement) {
            notice.remove();
        }
    }, 10000);
}

function showIOSNotice() {
    const downloadCard = document.querySelector('.download-card');
    const notice = document.createElement('div');
    notice.className = 'ios-notice';
    notice.innerHTML = `
        <div style="background: #FF6B6B; color: white; padding: 1rem; border-radius: 8px; margin-bottom: 1rem;">
            <strong>📱 Dispositivo iOS Detectado</strong>
            <p style="margin: 0.5rem 0 0 0; font-size: 0.9rem;">
                Esta aplicación es solo para Android. Si tienes un dispositivo Android, 
                comparte este enlace con él.
            </p>
        </div>
    `;
    
    downloadCard.insertBefore(notice, downloadCard.firstChild);
}

function trackDownload() {
    // Aquí puedes agregar tracking con Google Analytics, etc.
    if (typeof gtag !== 'undefined') {
        gtag('event', 'download', {
            'event_category': 'APK',
            'event_label': 'Almex Asistente',
            'value': 1
        });
    }
    
    console.log('📱 Descarga de Almex Asistente iniciada');
}

function showSupport() {
    alert(`
🤖 Soporte de Almex Asistente

📧 Email: soporte@almex-asistente.com
💬 Telegram: @AlmexSoporte
🐛 Reportar bugs: GitHub Issues

¿Problemas con la instalación?
1. Verifica que tengas Android 7.0+
2. Habilita "Fuentes desconocidas"
3. Libera al menos 50MB de espacio
4. Reinicia el teléfono si es necesario

¡Gracias por usar Almex! 🚀
    `);
}

// Agregar estilos CSS dinámicos
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    .notice-content {
        text-align: center;
        color: var(--text-primary);
    }
    
    .notice-content button {
        background: var(--accent-blue);
        color: white;
        border: none;
        padding: 0.5rem 1rem;
        border-radius: 20px;
        margin-top: 0.5rem;
        cursor: pointer;
        font-family: inherit;
    }
    
    .notice-icon {
        font-size: 2rem;
        display: block;
        margin-bottom: 0.5rem;
    }
`;
document.head.appendChild(style);