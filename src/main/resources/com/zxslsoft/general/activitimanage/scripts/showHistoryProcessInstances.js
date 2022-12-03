function showHistoryProcessInstancesFactory(historyProcesPane) {
    return () => {
        axios.get('showHistoryProcessInstances')
            .then(function (response) {
                const data = response.data || [];
                createOperationTable(historyProcesPane, undefined,
                    data
                );
            })
            .catch(function (error) {
                historyProcesPane.innerHTML = MSG_SYSTEM_ERROR;
            })
    }
}