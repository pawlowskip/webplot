###dependencies = [
    'App.controllers'
]

 app = angular.module('App', dependencies)
 @controllersModule = angular.module('App.controllers', [])
###

(->
    angular.module('App', ['ui.bootstrap', 'colorpicker.module'])
)()