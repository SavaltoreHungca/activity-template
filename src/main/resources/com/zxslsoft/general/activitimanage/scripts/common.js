EMPYT_F = function () { };

ERROR_F = function (error) {
    if (error) {
        console.log(error);
        if (typeof error === 'object') {
            showMessage(JSON.stringify(error, undefined, 2));
        } else {
            alert(error);
        }
    }
}

// Generate a pseudo-GUID by concatenating random hexadecimal. 
function guid() {
    // Generate four random hex digits. 
    function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    };
    return "_" + S4() + S4() + S4() + S4() + S4() + S4() + S4() + S4();
};

function anonyF(func) {
    const f = guid();
    window[f] = func;
    return f;
}

function confirmPane(fields, onConfirm) {
    const baseStyle = `
        position: fixed;
        left: 0;
        top: 0;
        width: 100vw;
        height: 100vh;
        z-index: 98;
        background: rgba(0,0,0,0.8);
        color: white;
    `
    let confirmPane = document.getElementById("confirmPane");
    if (!confirmPane) {
        confirmPane = document.createElement('div');
        confirmPane.id = 'confirmPane';
        confirmPane.style.cssText = baseStyle + "display: none;";
        document.body.appendChild(confirmPane);
    }

    confirmPane.style.cssText = baseStyle

    confirmPane.fieldMap = {};
    let innerH = '<table style="margin: auto; margin-top: 10vh">'
    for (const name of fields) {
        innerH += `
            <tr>
                <td>${name}</td>
                <td><input onchange="${anonyF((self) => { confirmPane.fieldMap[name] = self.value; })}(this)"></td>
            </tr>
        `
    }

    const cancelAct = () => {
        confirmPane.style.cssText = baseStyle + "display: none;";
    }
    const buttonAct = () => {
        onConfirm(confirmPane.fieldMap);
        cancelAct();
    }
    innerH += `
        <tr>
            <td style="background: none"></td>
            <td>
                <button onclick="${anonyF(cancelAct)}()">cancel</button>
                <button onclick="${anonyF(buttonAct)}()">confirm</button>
            </td>
        </tr>
    `
    innerH += '</table>';
    confirmPane.innerHTML = innerH;
}

function isOnLoading(isOnloading, msg) {
    const baseStyle = `
        position: fixed;
        left: 0;
        top: 0;
        width: 100vw;
        height: 100vh;
        z-index: 99;
        background: rgba(0,0,0);
        color: white;
    `
    let loadingPane = document.getElementById("loadingPane");
    if (!loadingPane) {
        loadingPane = document.createElement('div');
        loadingPane.id = 'loadingPane';
        loadingPane.style.cssText = baseStyle + "display: none;";
        document.body.appendChild(loadingPane);
    }
    if (isOnloading) {
        loadingPane.innerHTML = `
            <h3 style="margin-top: 50vh">
                ${msg ? msg : 'Processing, please wait...'}
            </h3>
        `;
        loadingPane.style.cssText = baseStyle;
    } else {
        loadingPane.style.cssText = baseStyle + "display: none;";
    }
}

function showMessage(msg) {
    console.log(msg);
    const baseStyle = `
        position: fixed;
        left: 0;
        top: 0;
        width: 100vw;
        height: 100vh;
        z-index: 99;
        background: rgba(0,0,0);
        color: white;
    `
    let msgPane = document.getElementById("msgPane");
    if (!msgPane) {
        msgPane = document.createElement('div');
        msgPane.id = 'msgPane';
        msgPane.style.cssText = baseStyle + "display: none;";
        document.body.appendChild(msgPane);
        msgPane.onclick = () => {
            msgPane.style.cssText = baseStyle + "display: none;";
        }
    }

    msgPane.innerHTML = `
        <h3 style="margin-top: 10px">
            <script type="text/html" style="display: block;">
                <pre>${typeof msg === 'object' ? JSON.stringify(msg, undefined, 2) : msg}</pre>
            </script>
        </h3>
    `;
    msgPane.style.cssText = baseStyle;
}

function picView(src) {
    const baseStyle = `
        position: fixed;
        left: 0;
        top: 0;
        width: 100vw;
        height: 100vh;
        z-index: 99;
        background: rgba(0,0,0);
        color: white;
    `
    let picPane = document.getElementById("picPane");
    if (!picPane) {
        picPane = document.createElement('div');
        picPane.id = 'picPane';
        picPane.style.cssText = baseStyle + "display: none;";
        document.body.appendChild(picPane);
        picPane.onclick = () => {
            picPane.style.cssText = baseStyle + "display: none;";
        }
    }

    picPane.innerHTML = `
        <img style="display: block;margin: auto; margin-top: 10px;" src="${src}"></img>
    `;
    picPane.style.cssText = baseStyle;
}

function createOperationTable(element, displayFields, data, operationBehaviorMap) {
    if(
        typeof data === 'undefined' || data === null
        || (Array.isArray(data) && data.length === 0)
    ){
        element.innerHTML = `<pre style="color: grey">
            No Data
        </pre>`
        return;
    }

    let tableHeader = '';
    const names = Array.isArray(displayFields) ? displayFields : [];

    if (Array.isArray(displayFields)) {
        for (const name of names) {
            tableHeader += `<td>${name}</td>`
        }
    } else {
        for (const name in data[0]) {
            if (data[0][name] !== 'object') {
                names.push(name);
                tableHeader += `<td>${name}</td>`
            }
        }
    }

    let innerH = `
        <table>
            <thead>
                <tr style="background: lightgreen;">${tableHeader}${operationBehaviorMap ? '<td>operations</td>' : ''}</tr>
            </thead>
        <tbody>`;
    for (const item of data) {
        innerH += '<tr>'
        for (const name of names) {
            innerH += `<td>${item[name]}</td>`
        }
        if (operationBehaviorMap) {
            innerH += `<td>`
            for (const operation in operationBehaviorMap) {
                innerH += `<button onclick="${anonyF(operationBehaviorMap[operation](item))}()">${operation}</button>`
            }
            innerH += `</td>`
        }
        innerH += '</tr>';
    }
    innerH += `</tbody></table>`;
    element.innerHTML = innerH;
}