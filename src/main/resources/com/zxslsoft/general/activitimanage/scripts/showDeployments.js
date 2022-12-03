function showDeploymentsFactory(deployPane) {
    return () => {
        axios.get('showDeployments')
            .then(function(response) {
                const data = response.data || [];

                createOperationTable(
                    deployPane, ['id', 'name'],
                    data
                )
            })
            .catch(function(error) {
                deployPane.innerHTML = MSG_SYSTEM_ERROR;
            })
    }
}