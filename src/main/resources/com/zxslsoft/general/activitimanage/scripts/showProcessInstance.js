function showProcessInstanceFactory(processPane) {
    return () => {
        axios.get('showProcessInstances')
            .then(function(response) {
                const data = response.data || [];
                createOperationTable(processPane, undefined,
                    data,
                );
            })
            .catch(function(error) {
                processPane.innerHTML = MSG_SYSTEM_ERROR;
            })
    }
}