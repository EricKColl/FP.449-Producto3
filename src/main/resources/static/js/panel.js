/*
 * Gráficas del panel del administrador (ECharts, servido como WebJar).
 * Cada gráfica lee sus datos de la tabla oculta que la acompaña (#datos-<nombre>), así que los datos los genera
 * siempre Thymeleaf en el servidor y la página sigue siendo legible sin JavaScript.
 */
(() => {
    'use strict';

    const echarts = window.echarts;
    if (!echarts) return;
    const reducido = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const oro = '#d4a94a';
    const oroClaro = '#f0d48a';
    const verde = '#4fbf86';
    const rojo = '#d65a5a';
    const texto = '#a9a08c';
    const rejilla = 'rgba(212, 169, 74, 0.08)';
    const tonos = ['#d4a94a', '#4fbf86', '#5b8def', '#b07cd8', '#e08a4b', '#d65a5a', '#7fc8c8', '#c9b27c'];

    const filas = (nombre) => [...document.querySelectorAll(`#datos-${nombre} tbody tr`)]
        .map((fila) => [...fila.cells].map((celda) => celda.textContent.trim()));

    const base = {
        animation: !reducido,
        animationDuration: 1200,
        animationEasing: 'cubicOut',
        textStyle: { fontFamily: 'Inter, system-ui, sans-serif', color: texto },
        tooltip: {
            backgroundColor: 'rgba(18, 17, 21, 0.96)',
            borderColor: 'rgba(212, 169, 74, 0.35)',
            textStyle: { color: '#ede6d6' }
        }
    };

    const degradado = (arriba, abajo) => new echarts.graphic.LinearGradient(0, 0, 0, 1,
        [{ offset: 0, color: arriba }, { offset: 1, color: abajo }]);

    const opciones = {
        actividad: (datos) => ({
            tooltip: { trigger: 'axis' },
            grid: { left: 36, right: 16, top: 16, bottom: 28 },
            xAxis: {
                type: 'category', data: datos.map((d) => d[0]),
                axisLine: { lineStyle: { color: rejilla } }, axisTick: { show: false }
            },
            yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: rejilla } } },
            series: [
                {
                    name: 'Préstamos', type: 'bar', barWidth: '38%', data: datos.map((d) => +d[1]),
                    itemStyle: { borderRadius: [8, 8, 0, 0], color: degradado(oroClaro, 'rgba(212, 169, 74, 0.25)') }
                },
                {
                    name: 'Devoluciones', type: 'line', smooth: true, symbolSize: 8, data: datos.map((d) => +d[2]),
                    lineStyle: { width: 3, color: verde }, itemStyle: { color: verde },
                    areaStyle: { color: degradado('rgba(79, 191, 134, 0.25)', 'rgba(79, 191, 134, 0)') }
                }
            ]
        }),
        estados: (datos) => ({
            tooltip: { trigger: 'item', formatter: '{b}: {c} ({d} %)' },
            legend: { bottom: 0, textStyle: { color: texto }, icon: 'circle' },
            series: [{
                type: 'pie', radius: ['52%', '78%'], center: ['50%', '45%'], padAngle: 3,
                itemStyle: { borderRadius: 8 },
                label: { show: true, position: 'center', formatter: () => `${datos.reduce((s, d) => s + +d[1], 0)}\npréstamos`,
                    color: '#f6efdf', fontSize: 18, fontFamily: 'Playfair Display, Georgia, serif', lineHeight: 22 },
                data: datos.map((d, i) => ({ name: d[0], value: +d[1], itemStyle: { color: [oro, rojo, verde][i] } }))
            }]
        }),
        libros: (datos) => ({
            tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
            grid: { left: 8, right: 28, top: 8, bottom: 8, containLabel: true },
            xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: rejilla } } },
            yAxis: {
                type: 'category', inverse: true, data: datos.map((d) => d[0]),
                axisLine: { show: false }, axisTick: { show: false },
                axisLabel: { width: 140, overflow: 'truncate', color: '#ede6d6' }
            },
            series: [{
                type: 'bar', data: datos.map((d) => +d[1]), barWidth: 14,
                label: { show: true, position: 'right', color: oroClaro },
                itemStyle: { borderRadius: 7, color: new echarts.graphic.LinearGradient(0, 0, 1, 0,
                    [{ offset: 0, color: 'rgba(212, 169, 74, 0.35)' }, { offset: 1, color: oroClaro }]) }
            }]
        }),
        generos: (datos) => ({
            tooltip: { trigger: 'item', formatter: '{b}: {c} títulos ({d} %)' },
            series: [{
                type: 'pie', roseType: 'radius', radius: ['18%', '78%'], center: ['50%', '50%'],
                itemStyle: { borderRadius: 6, borderColor: '#131216', borderWidth: 2 },
                label: { color: '#ede6d6', formatter: '{b}\n{c}' },
                labelLine: { lineStyle: { color: 'rgba(212, 169, 74, 0.4)' } },
                data: datos.map((d, i) => ({ name: d[0], value: +d[1], itemStyle: { color: tonos[i % tonos.length] } }))
            }]
        })
    };

    const graficas = [];
    document.querySelectorAll('[data-grafica]').forEach((contenedor) => {
        const nombre = contenedor.dataset.grafica;
        const datos = filas(nombre);
        if (!opciones[nombre] || datos.length === 0) {
            contenedor.classList.add('grafica-vacia');
            contenedor.textContent = 'Todavía no hay datos.';
            return;
        }
        const grafica = echarts.init(contenedor, null, { renderer: 'canvas' });
        grafica.setOption({ ...base, ...opciones[nombre](datos) });
        graficas.push(grafica);
    });

    new ResizeObserver(() => graficas.forEach((g) => g.resize())).observe(document.body);
})();
