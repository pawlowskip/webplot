(->

    FilesCtrl = ($scope, $http) ->

        @modalFileView = undefined
        @files = []

        ## For file uploading
        uploadSettings =
            url: 'upload'
            dataType: 'json'
            done: (e, data) => @getFiles()
            progressall: (e, data) ->
                progress = parseInt(data.loaded / data.total * 100, 10)
                $('#progress .progress-bar').css('width',progress + '%')

        init = () =>
                @getFiles()
                $('#fileupload').fileupload(uploadSettings)

        @refreshFiles = (data, status) =>
                            @files = data
                            $scope.$apply()

        @getFiles = () => $.get '/getFiles', @refreshFiles

        @deleteFile = (f) => $.get '/deleteFile', { file: f.name }, () => @getFiles()

        @downloadFile = (f) =>
                ref = '/downloadFile?' + $.param({file: f.name})
                $('#download').attr({target: '_blank', href  : ref})
                $('#download')[0].click()

        @viewFile = (file) => @modalFileView = file

        @resetFileProgressBar = () -> $('#progress .progress-bar').css('width',0 + '%')

        init()
        return

    angular
        .module('App')
        .controller('FilesCtrl' , FilesCtrl)
)()
