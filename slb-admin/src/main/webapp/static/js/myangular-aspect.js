module
    .controller(
    'AspectController',
    function ($scope, DataService, $resource, $http) {
        var Aspects = $resource(window.contextpath
            + '/console/base/listAspects');
        $scope.aspects = Aspects.query(function () {
            // 展现出来
            $('#AspectController > div.main-content').show();
            // 开始监听pool的修改
            $scope.$watch('aspects', function (newValue, oldValue) {
                if (newValue != oldValue) {
                    aspectChanged = true;
                }
            }, true);
        });
        // aspect视图切换
        $scope.aspectEditing = null;
        $scope.switchAspect = function (index) {
            $scope.aspectEditing = $scope.aspects[index];
        };
        $scope.switchAspectList = function () {
            $scope.aspectEditing = null;
        }
        $scope.edit = function () {
            window.location = window.contextpath + '/console/aspect/edit';
        }
        $scope.cancleEdit = function () {
            window.location = window.contextpath + '/console/aspect';
        }
        var aspectChanged = false;
        // 保存
        $scope.save = function () {
            $http({
                method: 'POST',
                data: $scope.aspects,
                url: window.contextpath + '/console/aspect/save'
            })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        app
                            .alertSuccess("保存成功！ 即将刷新页面...");
                        aspectChanged = false;// 保存成功，修改标识重置
                        setTimeout(
                            function () {
                                window.location = window.contextpath
                                    + "/console/aspect"
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
        // aspect增删
        $scope.openAddAspectModal = function () {
            $scope.aspectToBeAdd = new Object();
            $scope.aspectToBeAdd.pointCut = "BEFORE";
            $('#addAspectModal').modal('show');
            $('#addAspectName').focus();
        };
        $scope.addAspect = function () {
            var name = $scope.aspectToBeAdd.name;
            if (name == null || name.trim() == '') {
                app.alertError("规则名必填！", "addAspectAlertDiv");
                return;
            }
            $scope.aspects.push($scope.aspectToBeAdd);
            $('#addAspectModal').modal('hide');
        }
        $scope.affirmRemoveAspectModal = function (index) {
            $scope.aspectToBeRemove = $scope.aspects[index];
            $scope.aspectIndexToBeRemove = index;
            $('#affirmRemoveAspectModal').modal('show');
        }
        $scope.removeAspect = function () {
            $scope.aspects.splice($scope.aspectIndexToBeRemove, 1);
            $('#affirmRemoveAspectModal').modal('hide');
        }
        // directive增删
        $scope.directiveDefinedInputs = DataService
            .getDirectiveDefinedInputs();
        $scope.getInputs = function (type) {
            return $scope.directiveDefinedInputs[type];
        }
        $scope.getValueList = function (input) {
            if (!input) {
                return;
            }
            if (input.name == 'pool-name') {// 对pool-name特殊处理
                var list = [];
                for (var i = 0; i < $scope.pools.length; i++) {
                    list.push($scope.pools[i].name);
                }
                //唤醒chosen-select
                setTimeout(function () {
                    $(".chosen-select").chosen()
                }, 1500);//由于angularjs加载option也是延迟的，所以这个操作得更迟才行。
                return list;
            }
            return input.valueList;
        }
        $scope.openAddDirectiveModal = function () {
            $scope.directiveToBeAdd = new Object();
            $scope.directiveToBeAdd.dynamicAttributes = {};
            for (first in $scope.directiveDefinedInputs)
                break;
            $scope.directiveToBeAdd.type = first;
            $('#addDirectiveModal').modal('show');
            $('#addDirectiveType').focus();
        };
        $scope.addDirective = function () {
            var directives = $scope.aspectEditing.directives;
            if (!directives) {
                directives = [];
                $scope.aspectEditing.directives = directives;
            }
            // 根据勾选的type，找到inputs模板
            var inputs = $scope.directiveDefinedInputs[$scope.directiveToBeAdd.type];
            // 给$scope.directiveToBeAdd赋予空的inputs模板的键值对
            for (var name in inputs) {
                $scope.directiveToBeAdd.dynamicAttributes[name] = "";
            }
            directives.push($scope.directiveToBeAdd);
            $('#addDirectiveModal').modal('hide');
        }
        $scope.affirmRemoveDirectiveModal = function (index) {
            $scope.directiveToBeRemove = $scope.aspectEditing.directives[index];
            $scope.directiveIndexToBeRemove = index;
            $('#affirmRemoveDirectiveModal').modal('show');
        }
        $scope.removeDirective = function () {
            $scope.aspectEditing.directives.splice(
                $scope.directiveIndexToBeRemove, 1);
            $('#affirmRemoveDirectiveModal').modal('hide');
        }
        // 指令下的属性的增删
        $scope.addDynamicAttribute = function (directive, name) {
            if (directive.dynamicAttributes[name] != null) {
                app
                    .appError('通知', "该参数名( " + name
                    + " )已经存在，不能添加！");
            } else {
                directive.dynamicAttributes[name] = '';
            }
        }
        $scope.removeDynamicAttribute = function (directive, name) {
            delete directive.dynamicAttributes[name];
        }
        // pool-name选择
        $scope.pools = DataService.getPools();

        // 离开页面时，对比一下vs是否发生了修改
        var onunload = function () {
            if (aspectChanged) {
                return "您的修改尚未保存，现在离开将丢失所有修改";
            }
        }
        window.onbeforeunload = onunload;
    });
