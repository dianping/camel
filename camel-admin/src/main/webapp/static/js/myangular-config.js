module.controller("ConfigController", function ($scope, DataService, $resource,
                                                $http) {
    $scope.addOrEdit = "add";
    $scope.isUserListReady = false;
    $scope.currentUser = {isAdmin: "false"};
    $scope.users = [];
    $scope.originAccount = "";

    $scope.listUsers = function () {
        $scope.isUserListReady = false;
        $http({
            method: 'GET',
            url: window.contextpath + '/config/member/list'
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                $scope.users = data.users;
                $scope.isUserListReady = true;
            } else {
                alert("请刷新：获取用户列表失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            alert("请刷新：响应错误", data);
        });
    }

    $scope.add = function (index) {
        $scope.addOrEdit = "add";
        $scope.currentUser = {isAdmin: "false"};
    }

    $scope.edit = function (index) {
        $scope.addOrEdit = "edit";
        $scope.currentUser = $scope.users[index];
        $scope.originAccount = $scope.users[index].account;
    }

    $scope.remove = function (index) {
        var account = $scope.users[index].account;

        $http(
            {
                method: 'DELETE',
                url: window.contextpath + '/config/member/delete/'
                + encodeURI(account)
            }).success(function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    $scope.listUsers();
                } else {
                    alert("请重试：删除失败: " + data.errorMessage);
                }
            }).error(function (data, status, headers, config) {
                alert("请重试：响应错误", data);
            });
    }

    $scope.addSubmit = function () {
        $http({
            method: 'POST',
            data: $scope.currentUser,
            url: window.contextpath + '/config/member/addMember'
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                $scope.listUsers();
                $scope.currentUser = {};
                $scope.addOrEdit = "add";
            } else {
                alert("请重试：添加失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            alert("请重试：响应错误", data);
        });
    }

    $scope.updateSubmit = function () {
        $http(
            {
                method: 'POST',
                data: $scope.currentUser,
                url: window.contextpath + '/config/member/updateMember/'
                + encodeURI($scope.originAccount)
            }).success(function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    $scope.listUsers();
                    $scope.currentUser = {};
                    $scope.addOrEdit = "add";
                } else {
                    alert("请重试：添加失败: " + data.errorMessage);
                }
            }).error(function (data, status, headers, config) {
                alert("请重试：响应错误", data);
            });
    }

    $scope.listUsers();
});