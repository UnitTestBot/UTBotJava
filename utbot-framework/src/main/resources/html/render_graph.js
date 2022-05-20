function show(graph, divId) {
    viz.renderSVGElement(graph).then(function(svg) {
        let div = document.getElementById(divId)
        let parent = div.parentNode

        parent.removeChild(div)
        div = document.createElement(divId);
        div.id = divId
        div.appendChild(svg)
        parent.appendChild(div)
    })
    .catch(error => {
        viz = new Viz();
        console.error(error);
    });
}