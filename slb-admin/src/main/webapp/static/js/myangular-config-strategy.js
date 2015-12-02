module.controller("ConfigStrategyController", function ($scope, DataService,
                                                        $resource, $http) {
    $scope.isStrategyReady = false;
    $scope.strategyList = [];

    $scope.listStrategies = function () {
        $scope.isStrategyReady = false;
        $http({
            method: 'GET',
            url: window.contextpath + '/config/strategy/list'
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                $scope.strategyList = data.strategyList;
                $scope.isStrategyReady = true;
            } else {
                alert("读取错误，请重试", data);
            }
        }).error(function (data, status, headers, config) {
            alert("请刷新：响应错误", data);
        });
    }

    $scope.addStrategy = function () {
        $('#newStrategyName').val('');
        $('#newStrategyType').val('round-robin');
        $('#newStrategyArgument').val('');
        $('#addStrategyModal').modal('show');
        $('#newStrategyName').focus();
    }

    $scope.submitNewStrategy = function () {
        var strategy = {};

        strategy.name = $('#newStrategyName').val().trim();
        strategy.type = $('#newStrategyType').val().trim();
        strategy.version = 1;

        var rawArgument = $('#newStrategyArgument').val().trim();

        if (rawArgument != '') {
            strategy.dynamicAttributes = {};
            strategy.dynamicAttributes.target = rawArgument;
        }

        $http({
            method: 'PUT',
            url: window.contextpath + '/config/strategy/addStrategy',
            data: strategy
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                $scope.showMessage("add_result_div", "alert alert-success", "添加成功");
                setTimeout(function () {
                    window.location.href = "/config/strategy/index";
                }, 2000);
            } else {
                alert("请重试：添加失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            $scope.showMessage("add_result_div", "alert alert-error", "请重试：响应错误" + data);
            alert("请重试：响应错误", data);
        });
    }

    $scope.edit = function (index) {
        var currentStrategy = $scope.strategyList[index];

        if (currentStrategy != null) {
            $('#editedStrategyName').val(currentStrategy.name);
            $('#editedStrategyType').val(currentStrategy.type);
            $('#editedStrategyVersion').val(currentStrategy.version);
            var dynamicAttributes = currentStrategy.dynamicAttributes;

            if (dynamicAttributes != null) {
                $('#editedStrategyArgument').val(dynamicAttributes.target);
            } else {
                $('#editedStrategyArgument').val('');
            }
            $('#editStrategyModal').modal('show');
            $('#editedStrategyName').focus();
        } else {
            alert("请对一个有效策略进行编辑");
        }
    }

    $scope.submitEditedStrategy = function () {
        var strategy = {};

        strategy.name = $('#editedStrategyName').val().trim();
        strategy.type = $('#editedStrategyType').val().trim();
        strategy.version = $('#editedStrategyVersion').val().trim();

        var rawArgument = $('#editedStrategyArgument').val().trim();

        if (rawArgument != '') {
            strategy.dynamicAttributes = {};
            strategy.dynamicAttributes.target = rawArgument;
        }

        $http({
            method: 'PUT',
            url: window.contextpath + '/config/strategy/editStrategy',
            data: strategy
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                $scope.showMessage("edit_result_div", "alert alert-success", "编辑成功");
                setTimeout(function () {
                    window.location.href = "/config/strategy/index";
                }, 2000);
            } else {
                alert("请重试：编辑失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            $scope.showMessage("edit_result_div", "alert alert-error", "请重试：响应错误" + data);
            alert("请重试：响应错误", data);
        });
    }

    $scope.remove = function (index) {
        var currentStrategy = $scope.strategyList[index];

        if (currentStrategy != null) {
            var strategyName = currentStrategy.name;

            $http(
                {
                    method: 'DELETE',
                    url: window.contextpath + '/config/strategy/delete/' + encodeURI(strategyName)
                }).success(function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        $scope.showMessage("result_div", "alert alert-success", "删除成功");
                        setTimeout(function () {
                            window.location.href = "/config/strategy/index";
                        }, 2000);
                    } else {
                        alert("请重试：删除失败: " + data.errorMessage);
                    }
                }).error(function (data, status, headers, config) {
                    alert("请重试：响应错误", data);
                });
        } else {
            alert("请对一个有效策略进行删除操作");
        }
    }

    $scope.showMessage = function (divId, divClass, message) {
        var messageDiv = $("#" + divId);

        if (messageDiv.size() == 1) {
            messageDiv.find(".result_message").text(message);
            messageDiv.removeClass("hide").addClass(divClass);
            setTimeout(function () {
                messageDiv.addClass("hide").removeClass(divClass);
                messageDiv.find(".result_message").text("");
            }, 3000);
        } else {
            throw new Error("cannot find the message div!");
        }
    }

    $scope.listStrategies();
});