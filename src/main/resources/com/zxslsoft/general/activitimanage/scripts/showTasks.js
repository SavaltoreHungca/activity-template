function showTasksFactory(taskPane, refreshHistoryPanceF) {
    const showTasks = () => {
        axios.get('showTasks')
            .then(function (response) {
                const data = response.data || [];
                createOperationTable(taskPane, undefined,
                    data,
                    {
                        'finish task': (item) => {
                            return () => {
                                confirmPane(['comment'], (map) => {
                                    isOnLoading(true);
                                    axios.get(`completeTask?taskId=${encodeURIComponent(item.id)}` +
                                        `&comment=${encodeURIComponent(map.comment)}`
                                    ).then(function (response) {
                                        showTasks();
                                        if (refreshHistoryPanceF) refreshHistoryPanceF();
                                    }).catch(ERROR_F).then(() => isOnLoading(false));
                                })
                            }
                        }
                    }
                );
            })
            .catch(function (error) {
                taskPane.innerHTML = MSG_SYSTEM_ERROR;
            })
    }

    return showTasks;
}