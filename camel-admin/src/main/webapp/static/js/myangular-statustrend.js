module.controller('StatusTrendController', function ($scope, DataService, $resource, $http) {
    $scope.currentPool = 'all';

    $scope.changePool = function () {
        window.location = window.contextpath + '/monitor/status/data/' + $scope.currentPool;
    }

    $scope.generateChart = function () {
        $scope.generateSubChart("container5XX", 'chart5', '5XX');
        $scope.generateSubChart("container4XX", 'chart4', '4XX');
    }

    $scope.generateSubChart = function (divId, subChart, titleSuffix) {
        $http({
            method: 'GET',
            url: window.contextpath + '/monitor/' + encodeURIComponent($scope.currentPool) + '/data/get?startTime=' + encodeURIComponent($scope.startTime) + '&endTime=' + encodeURIComponent($scope.endTime)
        }).success(function (data, status, headers, config) {
            chart = data.charts[subChart];
            if (data.errorCode == 0) {
                $(function () {
                    $('#' + divId).highcharts({
                        title: {
                            text: chart.title + ' - ' + titleSuffix,
                            x: 0 // center
                        },
                        subtitle: {
                            text: chart.subTitle,
                            x: -20
                        },
                        xAxis: {
                            type: 'datetime'
                        },
                        yAxis: {
                            title: {
                                text: 'status count'
                            },
                            plotLines: [{
                                value: 0,
                                width: 10,
                                color: '#808080'
                            }]
                        },
                        tooltip: {
                            valueSuffix: ''
                        },
                        legend: {
                            layout: 'vertical',
                            align: 'right',
                            verticalAlign: 'middle',
                            borderWidth: 0
                        },
                        plotOptions: {
                            series: {
                                pointStart: chart.plotOption.series.pointStart + 8 * 3600 * 1000,
                                pointInterval: chart.plotOption.series.pointInterval // one day
                            }
                        },
                        series: chart.series
                    });
                });
            } else {
                alert("获取失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            alert("响应错误", data);
        });
    }
    // search
    $scope.pools = DataService.getPools(function () {
        var poolNameList = ['all'];
        $.each($scope.pools, function (i, pool) {
            poolNameList.push(pool.name);
        });
        $("#pool-search-nav").typeahead(
            {
                source: poolNameList,
                updater: function (c) {
                    window.location = window.contextpath
                        + '/monitor/status/data/' + c;
                    return c;
                }
            })
    });
    $scope.initMethod = function () {
        angular.element(document).ready(function () {
            $scope.generateChart();
        });
    };
    $scope.initMethod();
});