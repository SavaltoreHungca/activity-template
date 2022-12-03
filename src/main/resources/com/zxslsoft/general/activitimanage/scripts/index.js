const MSG_SYSTEM_ERROR = "SYSTEM_ERROR";

const container = document.createElement('div');
document.body.appendChild(container);

container.innerHTML = `
    <h4>Models</h4>
    <button id="createModelB">create model</button>
    <div id="models-pane"></div>
    <h4>Deployments</h4>
    <div id="deploy-pane"></div>
    <h4>Process Definitions</h4>
    <div id="definitions-pane"></div>
    <h4>Process Instances</h4>
    <div id="process-pane"></div>
    <h4>Task Pance</h4>
    <div id="task-pane"></div>
    <h4>History Process Pance</h4>
    <div id="history-process-pane"></div>
`


const deployPane = document.getElementById("deploy-pane"),
    modelPane = document.getElementById("models-pane"),
    createModelB = document.getElementById("createModelB"),
    definitionsPane = document.getElementById("definitions-pane"),
    processPane = document.getElementById("process-pane"),
    taskPane = document.getElementById("task-pane"),
    historyPane = document.getElementById('history-process-pane');

const showHistoryProcessInstances = showHistoryProcessInstancesFactory(historyPane);
const showTasks = showTasksFactory(taskPane, showHistoryProcessInstances);
const showDeployments = showDeploymentsFactory(deployPane);
const showProcessInstance = showProcessInstanceFactory(processPane);
const showProcessDefinitions = showProcessDefinitionsFactory(definitionsPane, showProcessInstance, showTasks);
const showModels = showModelsFactory(modelPane, showDeployments, showProcessDefinitions);


showModels();
showDeployments();
showProcessDefinitions();
showProcessInstance();
showTasks();
showHistoryProcessInstances();

createModelB.onclick = () => {
    confirmPane(['modelName', 'description'], (map) => {
        isOnLoading(true, 'Creating, please wait...')
        axios.get(`create?modelName=${encodeURIComponent(map.modelName)}` +
            `&description=${encodeURIComponent(map.description)}`
        )
            .then((response) => {
                showModels();
            }).catch(ERROR_F).then(() => isOnLoading(false));
    })
}