module
    .controller(
    'VariableController',
    function ($scope, DataService, $resource, $http) {

        var Variables = $resource(window.contextpath
            + '/console/variable/get');
        $scope.variables = Variables.query(function () {
            // 展现出来
            $('#VariableController > div.main-content').show();
            // 开始监听pool的修改
            $scope.$watch('variables', function (newValue, oldValue) {
                if (newValue != oldValue) {
                    variableChanged = true;
                }
            }, true);
        });

        $scope.edit = function () {
            window.location = window.contextpath + '/console/variable/edit';
        }
        $scope.cancleEdit = function () {
            window.location = window.contextpath + '/console/variable';
        }

        $scope.isShowInfluencingVs = false;
        $scope.showInfluencingVs = function (key) {
            $scope.influencingKey = key;
            $http({
                method: 'POST',
                url: window.contextpath + '/console/variable/' + key + '/influencingVs/get'
            })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {

                        $scope.isShowInfluencingVs = true;
                        $scope.influencingVs = data.influencingVs;

                    } else {
                        app.alertError("调用失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        }


        $scope.deployVariable = function (key) {
            app.alertProgress();
            $http({
                method: 'POST',
                url: window.contextpath + '/console/variable/' + key + '/deploy'
            })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {

                        window.location = window.contextpath
                            + "/console/apideploy/task/" + data.taskId
                            + window.location.hash;

                    } else {
                        app.alertError("发布失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        }

        var variableChanged = false;
        // 保存
        $scope.save = function () {
            $http({
                method: 'POST',
                data: $scope.variables,
                url: window.contextpath + '/console/variable/save'
            })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        app
                            .alertSuccess("保存成功！ 即将刷新页面...");
                        variableChanged = false;// 保存成功，修改标识重置
                        setTimeout(
                            function () {
                                window.location = window.contextpath
                                    + "/console/variable"
                                    + window.location.hash;
                            }, 700);
                    } else {
                        app.alertError("保存失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        };

        // variable增删
        $scope.openAddVariableModal = function () {
            $('#addVariableModal').modal('show');
            $('#addVariableName').focus();
        };

        $scope.openEditVariableModal = function (index) {
            $scope.variableToBeEdited = $scope.variables[index];
            $('#editVariableModal').modal('show');
            $('#editVariableName').focus();
        }
        $scope.editVariable = function () {
            $('#editVariableModal').modal('hide');
        }

        $scope.addVariable = function () {
            var key = $scope.variableToBeAdded.key;
            if (key == null || key.trim() == '') {
                app.alertError("变量名必填！" + key, "addVariableAlertDiv");
                return;
            }
            var value = $scope.variableToBeAdded.value;
            if (value == null || value.trim() == '') {
                app.alertError("变量值必填！", "addVariableAlertDiv");
                return;
            }
            var newValue = {};
            newValue.key = $scope.variableToBeAdded.key;
            newValue.value = $scope.variableToBeAdded.value;
            $scope.variables.push(newValue);
            $('#addVariableModal').modal('hide');
        }
        $scope.affirmRemoveVariableModal = function (index) {
            $scope.variableToBeRemove = $scope.variables[index];
            $scope.variableIndexToBeRemove = index;
            $('#affirmRemoveVariableModal').modal('show');
        }
        $scope.removeVariable = function () {
            $scope.variables.splice($scope.variableIndexToBeRemove, 1);
            $('#affirmRemoveVariableModal').modal('hide');
        }

        // 离开页面时，对比一下vs是否发生了修改
        var onunload = function () {
            if (variableChanged) {
                return "您的修改尚未保存，现在离开将丢失所有修改";
            }
        }
        window.onbeforeunload = onunload;
    });
