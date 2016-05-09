module.controller('VsController', function ($scope, DataService, $resource,
                                            $http) {
    //默认移动数值
    $scope.count = 1;
    $scope.selectedTab = 'profile';
    // 获取hash，设置给selectedTab
    var hash = window.location.hash;
    if (hash.length > 1) {// 去掉#号
        hash = hash.substring(1);
        if (hash == 'profile' || hash == 'aspect' || hash == 'location' || hash == 'diff') {
            $scope.selectedTab = hash;
        }
    }
    $scope.isActive = function (tabName) {
        var clazz = ($scope.selectedTab == tabName) ? 'active' : '';
        return clazz;
    };
    $scope.isActiveTabPanel = function (tabName) {
        var clazz = ($scope.selectedTab == tabName) ? 'tab-pane active'
            : 'tab-pane';
        return clazz;
    };
    $scope.isActive4Boolean = function (tabName) {
        var re = ($scope.selectedTab == tabName) ? true : false;
        setTimeout(function () {
            $scope.show_inited = true;
            $(".chosen-select").chosen();
        }, 4000);// 由于angularjs加载option也是延迟的，所以这个操作得更迟才行。
        return re;
    };
    var vsChanged = false;
    $scope.vs = null;
    $scope.tags = [];
    $scope.getVs = function (vsName) {
        $http({
            method: 'GET',
            url: window.contextpath + '/console/vs/' + vsName + '/get'
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                if (data.virtualServer == null) {// 新建vs
                    $scope.vs = new Object();
                    $scope.vs.name = vsName;
                    $scope.vs.domain = '.dianping.com';
                    $scope.vs.port = 80;
                    $scope.vs.dynamicAttributes = new Object();
                    $scope.vs.instances = [];
                    $scope.vs.instances.push(new Object());
                    $scope.vs.pools = [];
                    $scope.vs.state = 'ENABLED';
                    $scope.vs.availability = 'AVAILABLE';
                    $scope.vs.locations = [];
                    $scope.vs.aspects = [];
                    $scope.vs.group = '默认';
                    $scope.newVs = true;
                } else {
                    $scope.vs = data.virtualServer;
                    $scope.newVs = false;
                }
                // 展现出来
                $('#VsController > div.main-content').css('opacity', 1);
                // 开始监听vs的修改
                $scope.$watch('vs', function (newValue, oldValue) {
                    if (newValue != oldValue) {
                        vsChanged = true;
                    }
                }, true);
            } else {
                app.alertError("获取失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            app.appError("响应错误", data);
        });
        // 获取tag list
        $http({
            method: 'GET',
            url: window.contextpath + '/console/vs/' + vsName + '/tag/list'
        }).success(function (data, status, headers, config) {
            $scope.tags = data;
        }).error(function (data, status, headers, config) {
            app.appError("响应错误", data);
        });
    };
    // 保存
    $scope.save = function () {
        $http(
            {
                method: 'POST',
                data: $scope.vs,
                url: window.contextpath + '/console/vs/' + $scope.vs.name
                + '/save'
            }).success(
            function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    app.alertSuccess("保存成功！ 即将刷新页面...");
                    vsChanged = false;// 保存成功，修改标识重置
                    setTimeout(function () {
                        window.location = window.contextpath
                            + "/console/vs/" + $scope.vs.name
                            + window.location.hash;
                    }, 700);
                } else {
                    app.alertError("保存失败: " + data.errorMessage);
                }
            }).error(function (data, status, headers, config) {
                app.appError("响应错误", data);
            });
    };
    // 删除
    $scope.removeVirtualServer = function () {
        $http(
            {
                method: 'POST',
                url: window.contextpath + '/console/vs/' + $scope.vs.name
                + '/remove'
            }).success(
            function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    app.alertSuccess("删除成功！ 即将刷新页面...",
                        "removeVirtualServerAlertDiv");
                    setTimeout(function () {
                        window.location = window.contextpath + "/";
                    }, 700);
                } else {
                    app.alertError("删除失败: " + data.errorMessage,
                        "removeVirtualServerAlertDiv");
                }
            }).error(function (data, status, headers, config) {
                app.appError("响应错误", data);
            });
    };
    // 用於select
    $scope.getInitSelectValue = function (curValue, valueList, propertyName) {
        var re = curValue;
        if (propertyName) {
            if ((curValue == null || curValue == '') && valueList
                && valueList.length > 0)
                re = valueList[0][propertyName];
        } else {
            if ((curValue == null || curValue == '') && valueList
                && valueList.length > 0) {
                re = valueList[0];
            }
        }
        return re;
    }
    $scope.edit = function () {
        window.location = window.contextpath + '/console/vs/' + $scope.vs.name
            + '/edit' + window.location.hash;
    }
    $scope.cancleEdit = function () {
        window.location = window.contextpath + '/console/vs/' + $scope.vs.name
            + window.location.hash;
    }
    $scope.preview = function () {
        showPreviewModal();
        $http(
            {
                method: 'POST',
                data: $scope.vs,
                url: window.contextpath + '/console/vs/' + $scope.vs.name
                + '/preview'
            }).success(
            function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    window.nginxConfigEditor.setValue(data.nginxConfig);
                    window.nginxConfigEditor.moveCursorTo(0, 0);
                } else {
                    $('#nginxConfigEditor').hide();
                    app.alertError("预览失败: " + data.errorMessage,
                        "previewVirtualServerAlertDiv");
                }
            }).error(function (data, status, headers, config) {
                app.appError("响应错误", data);
            });
    }
    $scope.diff = function () {
        $scope.getTagAndDiff($scope.tagCompare1, 0);
        $scope.getTagAndDiff($scope.tagCompare2, 1);
    }
    $scope.getTagAndDiff = function (tagId, leftOrRight) {
        // 发起获取tag的请求
        $http(
            {
                method: 'GET',
                url: window.contextpath + '/console/vs/' + $scope.vs.name
                + '/tag/get/' + tagId
            }).success(function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    var viewType = 0;// 0 is side by side,1 is inline
                    if ($scope.inline == 1) {
                        viewType = 1;
                    }
                    var byId = function (id) {
                        return document.getElementById(id);
                    };
                    if (leftOrRight == 0) {
                        $scope.base = difflib.stringAsLines(data.nginxConfig);
                    } else {
                        $scope.newtxt = difflib.stringAsLines(data.nginxConfig);
                    }
                    if ($scope.base != null && $scope.newtxt != null) {
                        var sm = new difflib.SequenceMatcher($scope.base, $scope.newtxt);
                        var opcodes = sm.get_opcodes();
                        var diffoutputdiv = byId("diffoutput");
                        var contextSize = 200;

                        diffoutputdiv.innerHTML = "";
                        contextSize = contextSize || null;

                        diffoutputdiv.appendChild(diffview.buildView({
                            baseTextLines: $scope.base,
                            newTextLines: $scope.newtxt,
                            opcodes: opcodes,
                            baseTextName: "Base Text",
                            newTextName: "New Text",
                            contextSize: contextSize,
                            viewType: viewType
                        }));
                    }

                } else {
                    app.alertError("预览失败: " + data.errorMessage);
                }
            }).error(function (data, status, headers, config) {
                app.appError("响应错误", data);
            });
    }

    // addTag
    $scope.addingTag = false;
    $scope.newTag = null;
    $scope.addTag = function () {
        app.clearAlertMessage();
        app.alertProgress();
        $scope.addingTag = true;
        var param = new Object();
        // param.virtualServerName = $scope.vs.name;
        param.version = $scope.vs.version;
        $http(
            {
                method: 'POST',
                data: $.param(param),
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                url: window.contextpath + '/console/vs/' + $scope.vs.name
                + '/tag/add'
            }).success(function (data, status, headers, config) {
                $scope.addingTag = false;
                if (data.errorCode == 0) {
                    app.alertSuccess("创建发布版本成功！点击“已创建的发布版本”可查看。");
                    $('#tagsUl').addClass('open');
                    $scope.tags.unshift(data.tagId);
                    $scope.newTag = data.tagId;
                } else {
                    app.alertError("创建失败: " + data.errorMessage);
                }
            }).error(function (data, status, headers, config) {
                $scope.addingTag = false;
                app.appError("响应错误", data);
            });
    };
    $scope.viewTag = function (tagId) {
        showPreviewModal();
        $http(
            {
                method: 'GET',
                url: window.contextpath + '/console/vs/' + $scope.vs.name
                + '/tag/get/' + tagId
            }).success(
            function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    window.nginxConfigEditor.setValue(data.nginxConfig);
                    window.nginxConfigEditor.moveCursorTo(0, 0);
                } else {
                    $('#nginxConfigEditor').hide();
                    app.alertError("预览失败: " + data.errorMessage,
                        "previewVirtualServerAlertDiv");
                }
            }).error(function (data, status, headers, config) {
                app.appError("响应错误", data);
            });
    };
    var showPreviewModal = function () {
        // 显示modal
        var height = $(window).height();
        var modalBody = $('#previewVirtualServerModal div.modal-body');
        modalBody.css('height', height - 200);
        window.nginxConfigEditor.setValue("");
        $('#nginxConfigEditor').show();
        $('#previewVirtualServerAlertDiv').html('');
        $('#previewVirtualServerModal').modal('show');
    };

    // search
    $http({
        method: 'GET',
        url: window.contextpath + '/console/vs/list'
    }).success(function (data, status, headers, config) {
        var vsNameList = [];
        $.each(data, function (i, vs) {
            vsNameList.push(vs.name);
        });
        $("#vs-search-nav").typeahead({
            source: vsNameList,
            updater: function (c) {
                window.location = window.contextpath + "/console/vs/" + c;
                return c;
            }
        })
    }).error(function (data, status, headers, config) {
        // app.appError("响应错误", data);
    });

    $scope.downLocation = function (index) {
        $scope.setLocation(index, $scope.vs.locations.length - 1 - index);
    }

    $scope.topLocation = function (index) {
        $scope.setLocation(index, -index);
    }

    $scope.setLocation = function (index0, index1) {
        index1 += index0;
        if (index0 >= 0 && index1 >= 0 && index0 <= $scope.vs.locations.length
            && index1 <= $scope.vs.locations.length) {
            var location = $scope.vs.locations[index0];
            $scope.vs.locations.splice(index0, 1);
            $scope.vs.locations.splice(index1, 0, location);

            vsChanged = true;
        }
    }

    // 离开页面时，对比一下vs是否发生了修改
    var onunload = function () {
        if (vsChanged) {
            return "您的修改尚未保存，现在离开将丢失所有修改";
        }
    }
    window.onbeforeunload = onunload;

});
