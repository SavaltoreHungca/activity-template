function showModelsFactory(modelsPane, refreshDeploymentsF, refreshDefinitionsF) {
    const showModels = function() {
        axios.get('showModels')
            .then(function(response) {
                const data = response.data || [];

                createOperationTable(modelsPane, ['id', 'name', 'key', 'createTime'],
                    data, {
                        edit: (item) => {
                            return () => {
                                window.open(`${SETTINGS.workflowDesignTollUrl + "/modeler.html?modelId=" + item.id}`);
                            }
                        },
                        deploy: (item) => {
                            return () => {
                                isOnLoading(true);
                                axios.get(`deploy?modelId=${item.id}`)
                                    .then(function() {
                                        // refresh deployments
                                        if (refreshDeploymentsF) refreshDeploymentsF();
                                        if (refreshDefinitionsF) refreshDefinitionsF();
                                    })
                                    .catch(function(error) {
                                        showMessage(error);
                                    })
                                    .then(function() { isOnLoading(false); });
                            }
                        },
                        delete: (item) => {
                            return () => {
                                isOnLoading(true);
                                axios.get(`deleteModel?modelId=${item.id}`)
                                    .then(() => {
                                        showModels();
                                    }).catch(ERROR_F).then(() => { isOnLoading(false) });
                            }
                        },
                        'show model pic': (item) => {
                            return () => {
                                picView(`showModelPicture?modelId=${encodeURIComponent(item.id)}`);
                            }
                        },
                        'show xml define': (item) => {
                            return () => {
                                isOnLoading(true);
                                axios.get(`showModelXMLDefinition?modelId=${item.id}`)
                                    .then((response) => {
                                        showMessage(response.data);
                                    }).catch(ERROR_F).then(() => isOnLoading(false));
                            }
                        }
                    }
                )
            })
            .catch(function(error) {
                modelsPane.innerHTML = MSG_SYSTEM_ERROR;
            })
    }
    return showModels;
}