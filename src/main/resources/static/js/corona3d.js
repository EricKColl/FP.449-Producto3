/*
 * Corona de BackLord en 3D para la portada (Three.js, servido como WebJar).
 * Se construye por código: aro, puntas, perlas, piedras y terciopelo, con materiales físicos y un entorno de luz
 * propio para los reflejos del oro. Gira despacio, flota y se inclina hacia el ratón; alrededor flota polvo dorado.
 * Si el navegador no tiene WebGL se queda la imagen plana de la corona; con «reducir movimiento» no se anima.
 */
import * as THREE from '/webjars/three/build/three.module.js';

const lienzo = document.querySelector('[data-corona-3d]');
const escenaHtml = document.querySelector('.escena-corona');
const reducido = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

if (lienzo && escenaHtml) {
    try {
        iniciar();
    } catch (error) {
        // Sin WebGL: se mantiene la corona en imagen
        escenaHtml.classList.add('sin-3d');
    }
}

function iniciar() {
    const render = new THREE.WebGLRenderer({ canvas: lienzo, antialias: true, alpha: true, powerPreference: 'high-performance' });
    let resolucion = Math.min(window.devicePixelRatio, 1.75);
    render.setPixelRatio(resolucion);
    render.outputColorSpace = THREE.SRGBColorSpace;
    render.toneMapping = THREE.ACESFilmicToneMapping;
    render.toneMappingExposure = 1.15;

    const escena = new THREE.Scene();
    const camara = new THREE.PerspectiveCamera(32, 1, 0.1, 100);
    camara.position.set(0, 1.3, 7.1);
    camara.lookAt(0, 0.45, 0);

    escena.environment = entornoDeLuz(render);
    const clave = new THREE.DirectionalLight(0xfff1d6, 2.2);
    clave.position.set(3, 5, 4);
    const contra = new THREE.DirectionalLight(0xd4a94a, 1.6);
    contra.position.set(-4, 2, -3);
    escena.add(clave, contra, new THREE.AmbientLight(0x2a2014, 0.6));

    const corona = construirCorona();
    escena.add(corona);
    const polvo = construirPolvo();
    escena.add(polvo);
    escena.add(construirHalo());

    // Inclinación hacia el ratón, suavizada
    const raton = { x: 0, y: 0 };
    window.addEventListener('pointermove', (e) => {
        raton.x = (e.clientX / window.innerWidth) * 2 - 1;
        raton.y = (e.clientY / window.innerHeight) * 2 - 1;
    }, { passive: true });

    const ajustar = () => {
        const { clientWidth: ancho, clientHeight: alto } = lienzo;
        if (!ancho || !alto) return;
        render.setSize(ancho, alto, false);
        camara.aspect = ancho / alto;
        camara.updateProjectionMatrix();
    };
    new ResizeObserver(ajustar).observe(lienzo);
    ajustar();

    // Solo se dibuja mientras la portada está a la vista
    let visible = true;
    new IntersectionObserver(([entrada]) => { visible = entrada.isIntersecting; }).observe(lienzo);

    const inicio = performance.now();
    let primero = true;
    const dibujar = () => {
        const t = (performance.now() - inicio) / 1000;
        // Entrada: la corona crece y aparece durante el primer segundo y medio
        const entrada = reducido ? 1 : Math.min(1, t / 1.6);
        const suave = 1 - Math.pow(1 - entrada, 3);
        corona.scale.setScalar(1.12 * (0.72 + 0.28 * suave));
        corona.position.y = reducido ? 0 : Math.sin(t * 1.1) * 0.07;
        corona.rotation.y = reducido ? -0.5 : t * 0.32 + raton.x * 0.35;
        corona.rotation.x += ((reducido ? 0.18 : 0.16 + raton.y * 0.18) - corona.rotation.x) * 0.05;
        corona.rotation.z += ((reducido ? 0 : -raton.x * 0.08) - corona.rotation.z) * 0.05;
        if (!reducido) {
            polvo.rotation.y = t * 0.05;
            polvo.material.opacity = 0.55 + Math.sin(t * 1.7) * 0.15;
        }
        render.render(escena, camara);
        if (primero) {
            primero = false;
            escenaHtml.classList.add('con-3d');
        }
    };

    if (reducido) {
        dibujar();
        new ResizeObserver(() => dibujar()).observe(lienzo);
    } else {
        // Si el equipo no llega a unos 30 fotogramas por segundo se baja la resolución; si aun así no llega,
        // la corona se queda quieta y deja de consumir: la página tiene que ir fluida en cualquier ordenador
        let anterior = performance.now();
        let lentos = 0;
        let rebajas = 0;
        render.setAnimationLoop(() => {
            if (!visible || document.hidden) {
                anterior = performance.now();
                return;
            }
            dibujar();
            const ahora = performance.now();
            lentos = ahora - anterior > 34 ? lentos + 1 : Math.max(0, lentos - 1);
            anterior = ahora;
            if (lentos > 20) {
                lentos = 0;
                if (rebajas++ < 2) {
                    resolucion = Math.max(0.6, resolucion * 0.66);
                    render.setPixelRatio(resolucion);
                    ajustar();
                } else {
                    render.setAnimationLoop(null);
                }
            }
        });
    }
}

// Sala con paneles de luz cálida: de ella salen los reflejos del metal (sin cargar ninguna imagen)
function entornoDeLuz(render) {
    const sala = new THREE.Scene();
    sala.add(new THREE.Mesh(new THREE.BoxGeometry(30, 30, 30),
        new THREE.MeshBasicMaterial({ color: 0x0b0906, side: THREE.BackSide })));
    const panel = (ancho, alto, color, x, y, z) => {
        const malla = new THREE.Mesh(new THREE.PlaneGeometry(ancho, alto), new THREE.MeshBasicMaterial({ color }));
        malla.position.set(x, y, z);
        malla.lookAt(0, 0, 0);
        sala.add(malla);
    };
    panel(10, 4, new THREE.Color(6, 5.2, 3.8), 0, 9, 4);
    panel(6, 8, new THREE.Color(3.5, 2.6, 1.3), -10, 2, 2);
    panel(6, 8, new THREE.Color(2.4, 2.2, 2.0), 10, 3, -2);
    panel(14, 2, new THREE.Color(1.4, 1.0, 0.45), 0, -6, -8);
    panel(16, 7, new THREE.Color(2.6, 2.1, 1.4), 0, 3, 11);
    panel(20, 20, new THREE.Color(0.5, 0.36, 0.18), 0, -10, 0);
    const generador = new THREE.PMREMGenerator(render);
    const textura = generador.fromScene(sala, 0.035).texture;
    generador.dispose();
    return textura;
}

function construirCorona() {
    const corona = new THREE.Group();
    const oro = new THREE.MeshPhysicalMaterial({
        color: 0xf2c46b, metalness: 1, roughness: 0.3, clearcoat: 0.4, clearcoatRoughness: 0.2, envMapIntensity: 1.35
    });
    const oroMate = new THREE.MeshPhysicalMaterial({ color: 0xd9a845, metalness: 1, roughness: 0.42, envMapIntensity: 1.3 });
    const perla = new THREE.MeshPhysicalMaterial({
        color: 0xfff6e8, roughness: 0.18, clearcoat: 1, sheen: 1, sheenColor: new THREE.Color(0xffe2b8)
    });
    const piedra = (color) => new THREE.MeshPhysicalMaterial({
        color, metalness: 0.2, roughness: 0.08, clearcoat: 1, clearcoatRoughness: 0.05, ior: 2.2,
        emissive: color, emissiveIntensity: 0.35
    });
    const piedras = [piedra(0xc3152f), piedra(0x1f56d6), piedra(0x13a063)];

    // Aro con dos molduras
    const aro = new THREE.Mesh(new THREE.CylinderGeometry(1.34, 1.24, 0.9, 128, 1, true), oro);
    aro.material.side = THREE.DoubleSide;
    corona.add(aro);
    const moldura = (radio, grosor, y) => {
        const toro = new THREE.Mesh(new THREE.TorusGeometry(radio, grosor, 24, 160), oroMate);
        toro.rotation.x = Math.PI / 2;
        toro.position.y = y;
        corona.add(toro);
    };
    moldura(1.355, 0.065, 0.45);
    moldura(1.255, 0.075, -0.45);

    // Cuentas de oro a lo largo de la moldura superior
    const cuentas = new THREE.InstancedMesh(new THREE.SphereGeometry(0.032, 12, 8), oro, 84);
    const m = new THREE.Matrix4();
    for (let i = 0; i < 84; i++) {
        const a = (i / 84) * Math.PI * 2;
        m.makeTranslation(Math.sin(a) * 1.4, 0.52, Math.cos(a) * 1.4);
        cuentas.setMatrixAt(i, m);
    }
    corona.add(cuentas);

    // Ocho puntas con una perla en cada extremo
    const forma = new THREE.Shape();
    forma.moveTo(-0.34, 0);
    forma.quadraticCurveTo(-0.1, 0.32, 0, 1.0);
    forma.quadraticCurveTo(0.1, 0.32, 0.34, 0);
    forma.lineTo(-0.34, 0);
    const punta = new THREE.ExtrudeGeometry(forma, {
        depth: 0.07, bevelEnabled: true, bevelThickness: 0.035, bevelSize: 0.03, bevelSegments: 4, curveSegments: 24
    });
    punta.translate(0, 0, -0.035);
    const puntas = 8;
    for (let i = 0; i < puntas; i++) {
        const a = (i / puntas) * Math.PI * 2;
        const pieza = new THREE.Group();
        pieza.position.set(Math.sin(a) * 1.34, 0.46, Math.cos(a) * 1.34);
        pieza.rotation.order = 'YXZ';
        pieza.rotation.y = a;
        pieza.rotation.x = -0.14;
        const alta = i % 2 === 0 ? 1 : 0.72;
        const malla = new THREE.Mesh(punta, oro);
        malla.scale.set(1, alta, 1);
        pieza.add(malla);
        const bola = new THREE.Mesh(new THREE.SphereGeometry(i % 2 === 0 ? 0.11 : 0.085, 32, 16), perla);
        bola.position.set(0, alta * 1.02 + 0.06, 0);
        pieza.add(bola);
        corona.add(pieza);

        // Piedra preciosa en el aro, entre punta y punta
        const b = a + Math.PI / puntas;
        const gema = new THREE.Mesh(new THREE.OctahedronGeometry(0.13, 0), piedras[i % 3]);
        gema.scale.set(0.75, 1.15, 0.45);
        gema.position.set(Math.sin(b) * 1.33, -0.02, Math.cos(b) * 1.33);
        gema.rotation.y = b;
        corona.add(gema);
        const engaste = new THREE.Mesh(new THREE.TorusGeometry(0.135, 0.022, 12, 40), oroMate);
        engaste.position.copy(gema.position);
        engaste.rotation.y = b;
        corona.add(engaste);
    }

    // Terciopelo granate dentro, rematado con un orbe y una cruz
    const terciopelo = new THREE.Mesh(
        new THREE.SphereGeometry(1.2, 64, 32, 0, Math.PI * 2, 0, Math.PI / 2),
        new THREE.MeshPhysicalMaterial({
            color: 0x4a0a16, roughness: 0.85, sheen: 1, sheenRoughness: 0.4, sheenColor: new THREE.Color(0xb3324a)
        }));
    terciopelo.scale.set(1, 0.72, 1);
    terciopelo.position.y = 0.38;
    corona.add(terciopelo);
    const orbe = new THREE.Mesh(new THREE.SphereGeometry(0.17, 48, 24), oro);
    orbe.position.y = 1.4;
    corona.add(orbe);
    const brazo = new THREE.BoxGeometry(0.07, 0.36, 0.07);
    const cruzV = new THREE.Mesh(brazo, oro);
    cruzV.position.y = 1.72;
    const cruzH = new THREE.Mesh(brazo, oro);
    cruzH.rotation.z = Math.PI / 2;
    cruzH.position.y = 1.76;
    corona.add(cruzV, cruzH);

    corona.scale.setScalar(1.12);
    return corona;
}

// Partículas de polvo dorado alrededor de la corona
function construirPolvo() {
    const cantidad = 700;
    const posiciones = new Float32Array(cantidad * 3);
    for (let i = 0; i < cantidad; i++) {
        const radio = 2.1 + Math.random() * 2.8;
        const angulo = Math.random() * Math.PI * 2;
        posiciones[i * 3] = Math.cos(angulo) * radio;
        posiciones[i * 3 + 1] = (Math.random() - 0.35) * 3.6;
        posiciones[i * 3 + 2] = Math.sin(angulo) * radio;
    }
    const geometria = new THREE.BufferGeometry();
    geometria.setAttribute('position', new THREE.BufferAttribute(posiciones, 3));
    return new THREE.Points(geometria, new THREE.PointsMaterial({
        size: 0.055, map: destello(), color: 0xf0d48a, transparent: true, opacity: 0.6,
        depthWrite: false, blending: THREE.AdditiveBlending
    }));
}

// Resplandor bajo la corona
function construirHalo() {
    const halo = new THREE.Mesh(new THREE.PlaneGeometry(6, 6), new THREE.MeshBasicMaterial({
        map: destello(), color: 0xd4a94a, transparent: true, opacity: 0.35, depthWrite: false, blending: THREE.AdditiveBlending
    }));
    halo.rotation.x = -Math.PI / 2;
    halo.position.y = -0.62;
    return halo;
}

function destello() {
    const lado = 64;
    const lienzoTextura = document.createElement('canvas');
    lienzoTextura.width = lienzoTextura.height = lado;
    const ctx = lienzoTextura.getContext('2d');
    const degradado = ctx.createRadialGradient(lado / 2, lado / 2, 0, lado / 2, lado / 2, lado / 2);
    degradado.addColorStop(0, 'rgba(255,255,255,1)');
    degradado.addColorStop(0.35, 'rgba(255,230,170,0.55)');
    degradado.addColorStop(1, 'rgba(255,200,120,0)');
    ctx.fillStyle = degradado;
    ctx.fillRect(0, 0, lado, lado);
    const textura = new THREE.CanvasTexture(lienzoTextura);
    textura.colorSpace = THREE.SRGBColorSpace;
    return textura;
}
