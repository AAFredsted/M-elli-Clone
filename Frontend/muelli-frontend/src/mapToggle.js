const emit = (eventName, detail) => {
    const event = new CustomEvent(eventName, { detail });
    document.dispatchEvent(event);
};

const updateFeatureToggles = (features) => {
    if(features.length === 0){
        console.log("we enter here");
        return
    }

    const cycleToggles = document.getElementById("cycleToggles");
    if (!cycleToggles) {
        console.error("Element with id 'cycleToggles' not found.");
        return;
    }
    cycleToggles.innerHTML = '';


    const hasSubcycleIndex = features[0].get('subcycleIndex') !== undefined;

    if(hasSubcycleIndex) {
        features.forEach((cycle) => {
            const toggle = document.createElement('div');
            const id = cycle.get('subcycleIndex');
            const color = cycle.get('color');
            toggle.innerHTML = `
                    <input type="checkbox" id=subcycle${id} checked="true" style="cursor:pointer"}>
                    <label for="subcycle${id}">
                        Route ${Number(id) + 1}
                        <span id="color-indicator" style="background-color: ${color}"></span>
                    </label>
            `
            cycleToggles.appendChild(toggle);

            document.getElementById(`subcycle${id}`).addEventListener("change", function(event) {
                emit(`toggle${id}`, event.target.checked);
            });


        })
    } 
    else {
        const toggle = document.createElement('div');
        toggle.innerHTML = `
            <input type="checkbox" id=baseLayer checked="true" style="cursor:pointer"}>
            <label for="baseLayer}">
                Map Geometry
                <span id="color-indicator" style="background-color: #000000"></span>
            </label>
        `
        cycleToggles.appendChild(toggle);
        document.getElementById(`baseLayer`).addEventListener("change", function(event) {
            emit(`toggleBase`, event.target.checked);
        });

    }


}

export { updateFeatureToggles }