var module = angular.module('MyApp', ['ngResource']);

module.config(function ($locationProvider, $resourceProvider) {
    // configure html5 to get links working on jsfiddle
    $locationProvider.html5Mode(true);
});

module.directive('ngEnter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if (event.which === 13) {
                scope.$apply(function () {
                    scope.$eval(attrs.ngEnter);
                });
                event.preventDefault();
            }
        });
    };
});

module.controller('BatchTagController', function ($scope, $resource, $http) {
    $scope.vsList = [];
    $scope.groups = {};

    $scope.start = function () {
        $.each($scope.vsList, function (i, vs) {
            if (vs.selected == true) {
                var param = new Object();
                param.version = vs.version;
                vs.status = 'doing';
                $http({
                    method: 'POST',
                    data: $.param(param),
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    url: window.contextpath + '/console/vs/' + vs.name + '/tag/add'
                }).success(function (data, status, headers, config) {
                    vs.status = 'done';
                    if (data.errorCode == 0) {
                        vs.success = true;
                        vs.tag = data.tagId;
                    } else {
                        vs.success = false;
                        vs.errorMsg = data.errorMessage;
                    }
                }).error(function (data, status, headers, config) {
                    vs.status = 'done';
                    vs.success = false;
                    vs.errorMsg = data;
                });

            }
        });
    }

    $http({
        method: 'GET',
        url: window.contextpath + '/console/vs/list'
    }).success(function (data, status, headers, config) {
        $scope.vsList = data;
        // 将所有vs分group
        $.each($scope.vsList, function (i, vs) {
            var groupName = vs.group;
            if (!groupName) {
                groupName = 'default';
            }
            var group = $scope.groups[groupName];
            if (!group) {
                group = new Object();
                group.name = groupName;
                group.vsList = [];
                $scope.groups[groupName] = group;
            }
            group.vsList.push(vs);
        });
        // 初始化table
        setTimeout(function () {
            $.each($scope.groups, function (groupName, group) {
                console.log(groupName);
                $('#groupTable_' + groupName).dataTable({
                    "bPaginate": false,
                    "bLengthChange": false,
                    "bInfo": false,
                    "aoColumns": [{
                        "bSortable": false
                    }, null, {
                        "bSortable": false
                    }],
                    "aaSorting": [[1, 'asc']]
                });
                // $('#groupTable_' + groupName).dataTable();
                // var oTable1 = $('#aaa').dataTable();

            });
        }, 500);

    }).error(function (data, status, headers, config) {
        app.appError("响应错误", data);
    });

    $scope.check = function (vs) {
        if (vs.selected) {
            vs.selected = false;
        } else {
            vs.selected = true;
        }
    }
    $scope.checkAll = function (group) {
        $.each(group.vsList, function (i, vs) {
            vs.selected = true;
        });
        group.checkAll = true;
    }
    $scope.uncheckAll = function (group) {
        $.each(group.vsList, function (i, vs) {
            vs.selected = false;
        });
        group.checkAll = false;
    }
});
