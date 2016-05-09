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

module.factory('DataService', function ($resource) {
    var model = {};

    // var PropertiesDefinedInputs = $resource(window.contextpath
    // + '/base/propertiesDefinedInputs');
    // model.propertiesDefinedInputs = PropertiesDefinedInputs.get(function() {
    // });
    var PropertiesDefinedInputs = $resource(window.contextpath
        + '/console/base/propertiesDefinedInputs');
    model.getPropertiesDefinedInputs = function (func) {
        model.propertiesDefinedInputs = PropertiesDefinedInputs.get(func);
        return model.propertiesDefinedInputs;
    };

    // var DirectiveDefinedInputs = $resource(window.contextpath
    // + '/base/directiveDefinedInputs');
    // model.directiveDefinedInputs = DirectiveDefinedInputs.get(function() {
    // });
    var DirectiveDefinedInputs = $resource(window.contextpath
        + '/console/base/directiveDefinedInputs');
    model.getDirectiveDefinedInputs = function (func) {
        model.directiveDefinedInputs = DirectiveDefinedInputs.get(func);
        return model.directiveDefinedInputs;
    };

    // var Strategies = $resource(window.contextpath + '/base/listStrategies');
    // model.strategies = Strategies.query(function() {
    // });
    var Strategies = $resource(window.contextpath + '/console/base/listStrategies');
    model.getStrategies = function (func) {
        model.strategies = Strategies.query(func);
        return model.strategies;
    };

    // var Pools = $resource(window.contextpath + '/base/listPools');
    // model.pools = Pools.query(function() {
    // });
    var Pools = $resource(window.contextpath + '/console/base/listPools');
    model.getPools = function (func) {
        model.pools = Pools.query(func);
        return model.pools;
    };

    var SlbPools = $resource(window.contextpath + '/console/base/listSlbPools');
    model.getSlbPools = function (func) {
        model.slbPools = SlbPools.query(func);
        return model.slbPools;
    };

    // var Aspects = $resource(window.contextpath + '/base/listAspects');
    // model.aspects = Aspects.query(function() {
    // });
    var Aspects = $resource(window.contextpath + '/console/base/listAspects');
    model.getAspects = function (func) {
        model.aspects = Aspects.query(func);
        return model.aspects;
    };

    // list tag的resource
    // model.Tags = $resource(window.contextpath + '/vs/:vsName0/tag/list');

    return model;
});

function isIP(strIP) {
    if (!strIP) return false;
    var re = /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/g //匹配IP地址的正则表达式
    if (re.test(strIP)) {
        if (RegExp.$1 < 256 && RegExp.$2 < 256 && RegExp.$3 < 256 && RegExp.$4 < 256) return true;
    }
    return false;
}
