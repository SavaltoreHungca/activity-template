function showProcessDefinitionsFactory(definitionsPane, refreshProcessInstanceF, refreshTasksF) {
    return () => {
        axios.get('showProcessDefinitions')
            .then(function (response) {
                const data = response.data || [];
                createOperationTable(definitionsPane, ['id', 'suspensionState', 'resourceName', 'deploymentId'],
                    data, {
                    'start process': (item) => {
                        return () => {
                            axios.get(`start?processDefinitionId=${encodeURIComponent(item.id)}`)
                                .then(function (response) {
                                    // refresh processes
                                    if (refreshProcessInstanceF) refreshProcessInstanceF();
                                    if (refreshTasksF) refreshTasksF();
                                })
                        }
                    }
                }

                );
            })
            .catch(function (error) {
                definitionsPane.innerHTML = MSG_SYSTEM_ERROR;
            })
    }
}