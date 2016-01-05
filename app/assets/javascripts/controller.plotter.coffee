(->

    project = (name, graphs, pages, author) ->
        name: name
        graphs: graphs
        pages: pages
        author: author

    graphRef = (id, x, y, width, height) ->
        graphId: id
        x: x
        y: y
        width: width
        height: height

    page = (title, titleSize, width, height, fontName, fontSize, terminal, url, graphRefs) ->
        title: title
        titleSize: titleSize
        width: width
        height: height
        font:
            name: fontName
            size: fontSize
        terminal: terminal
        url: url
        graphRefs: graphRefs

    plot = (label, plotDataType, dataFun, dataFile, plotType, using, patternType, fillType, lineType, pointType, lineWidth, pointSize, color) ->
            label: label
            plotDataType: plotDataType
            dataFun: dataFun
            dataFile: dataFile
            plotType: plotType
            using: using
            patternType: patternType
            fillType: fillType
            lineType: lineType
            pointType: pointType
            lineWidth: lineWidth
            pointSize: pointSize
            color: color

    axis = (name, label, from, to, isLogScale) ->
        name: name
        label: label
        fromTo:
            from: from
            to: to
        isLogScale: isLogScale

    graph = (title, graphType, xAxis, yAxis, zAxis,  viewX, viewY, samplesX, samplesY, samplesZ, gridWidth, gridType,
            isUsed, place, horiPos, vertPos, adjust, box, width, height, posX, posY, contourFontSize,
            contourLevels, histogramType, histogramGap, histogramBoxWidth, histogramBorderType1, histogramBorderType2,
            histogramBorderColor, palette, plots) ->
        title: title
        graphType: graphType
        xAxis: xAxis
        yAxis: yAxis
        zAxis: zAxis
        view:
            viewX: viewX
            viewY: viewY
        samples:
            samplesX: samplesX
            samplesY: samplesY
            samplesZ: samplesZ
        grid:
            gridWidth: gridWidth
            gridType: gridType
        legend:
            isUsed: isUsed
            place: place
            horiPos: horiPos
            vertPos: vertPos
            adjust: adjust
            box: box
        width: width
        height: height
        position:
            posX: posX
            posY: posY
        contourFontSize: contourFontSize
        contourLevels: contourLevels
        histogramType: histogramType
        histogramGap: histogramGap
        histogramBoxWidth: histogramBoxWidth
        histogramBorder:
            type: [histogramBorderType1, histogramBorderType2]
            color: histogramBorderColor
        palette: palette
        plots: plots


    defColor = () => 'rgb(' + random(0, 255) + ',' + random(0, 255) + ',' +  random(0, 255) + ')'

    defaultAxis = (type) -> axis(type, type, '0', '1', false)

    defaultPlot = () -> plot('Label', 'f', 'x*x', '', 'lines', '', "1", "x1", "1", "1", "1", "1", defColor())

    defaultProject = (graphs) -> project('untitled', graphs, [defaultPage()], 'author')

    emptyProject = () -> (graphs) -> project('', graphs, [defaultPage()], 'author')

    defaultPage = () -> page("", '12', '500', '600', 'Cambria', '12', 'svg', 'url', [])

    defaultGraph = () -> graph('abc', '2D', defaultAxis("x"), defaultAxis("y"), defaultAxis("z"),
                            '45', '45', '30', '30', '30', '0.5', 'x y', 'on', 'inside', 'left', 'center', 'horizontal', 'box', '1.0', '1.0',
                            '0.0', '0.0', '0.7', '20', 'cluster', '1', '0.9', 'solid', 'border', defColor(),
                            [defColor(), defColor()], [defaultPlot()])

    createSinglePage = (graphs, width, height) -> page("", '10', width, height, 'Cambria', '12', 'svg', 'url', graphs)

    refreshUI = () -> 
        run = () ->
             $('.selectpicker').selectpicker({size: 5})
             $('[data-toggle="popover"]').popover({html: true})
             $('[data-toggle="tooltip"]').tooltip()
        setTimeout(run, 10)

    random = (from, to) -> Math.floor(Math.random() * (to - from) ) + from

    goToGraph = (idx) -> $('#plot-pagination a[href="#tab-' + (idx) + '"]').tab('show')

    goToPage = (idx) -> $('#page-pagination a[href="#tab-page' + (idx) + '"]').tab('show')



    Plotter = ($scope, $http, $q) ->

        @graphHeaders = {'Accept': 'image/svg+xml', 'Content-Type': 'image/svg+xml'}
        @projectHeaders = {'Accept': 'application/zip', 'Content-Type': 'application/zip'}
        @modalFileView = undefined
        @usingTipDescr = ''
        @newProjectName = "untitled"
        @selectedOption = undefined
        @saveAsWidth = 500
        @saveAsHeight = 600
        ##@selectedPage = 'Page 1'
        @modalFileView = undefined
        @graphs = []
        @project = undefined
        @projectList = []
        @projectSelectList = []
        @files = []
        @pagePosition = undefined
        @originPosX = undefined
        @originPosY = undefined
        @isCalculated = false
        @containerW = 0
        @containerH = 0

        init = () =>
            $.get '/userFiles', @refreshFiles
            $.get '/getProjects', @refreshProjects
            refreshUI()
            ## hidden pagination
            run = () =>
                $('#pagination').removeAttr("hidden")
            setTimeout(run, 20)
            window.dragMoveListener = dragMoveListener

        @getNumber = (n) => [1..n]

        @sendMessage = (x)=> alert (x)

        @viewFile = (g, p) =>
            name = @graphs[g].plots[p].dataFile
            file = (x for x in @files when x.name == name )[0]
            @modalFileView = file

        @addColor = (grId) =>
            @graphs[grId].palette.push(defColor())

        @removeColor = (graphIndex, colorIndex) =>
            if @graphs[graphIndex].palette.length > 2
                array = @graphs[graphIndex].palette
                @graphs[graphIndex].palette = (x for x in array when x != array[colorIndex])

        @changeGraphType = (graphIndex, typ) =>
            if typ == 'HIST'
                for index in [0..(@graphs[graphIndex].plots.length-1)]
                    do =>
                        @graphs[graphIndex].plots[index].plotDataType = 'd'
            else if typ == '3D'
                @changeUsingWarning(2)
                for index in [0..(@graphs[graphIndex].plots.length-1)]
                    do =>
                        plotType = @graphs[graphIndex].plots[index].plotType
                        if !(plotType == 'points' || plotType == 'lines' || plotType == 'linespoints')
                            @graphs[graphIndex].plots[index].plotType = 'lines'
            else
                @changeUsingWarning(1)
            @graphs[graphIndex].graphType = typ

        @changePlotType = (graphIdx, plotIdx, plotType) =>
            if plotType.indexOf('error') > -1
                @graphs[graphIdx].plots[plotIdx].plotDataType = 'd'
                if plotType == 'xyerrorbars'
                    @changeUsingWarning(4)
                else @changeUsingWarning(3)
            else
                @changeUsingWarning(1)
            @graphs[graphIdx].plots[plotIdx].plotType = plotType

        @changePlotDataType = (graphIdx, plotIdx, plotDataType) =>
            if plotDataType == 'd' || @graphs[graphIdx].plots[plotIdx].plotType.indexOf('error') == -1
                @graphs[graphIdx].plots[plotIdx].plotDataType = plotDataType

        @newPlotInGraph = (index) =>
            @graphs[index].plots.push(defaultPlot())
            refreshUI()

        @removePlotFromGraph = (plotIdx, graphIdx) =>
           array = @graphs[graphIdx].plots
           @graphs[graphIdx].plots = (x for x in array when x != array[plotIdx])

        @newGraph = () =>
           @graphs.push(defaultGraph())
           idx = (@graphs.length - 1)
           run = () ->
                goToGraph(idx)
           setTimeout(run, 20)
           refreshUI()

        @removeGraph = (index) =>
            total = @graphs.length
            if total > 1
                array = @graphs
                @graphs = (x for x in array when x != array[index])
                run = () ->
                    if index == total - 1
                        goToGraph(index - 1)
                    else
                        goToGraph(index)
                setTimeout(run, 20)

        @newPage = () =>
           @project.pages.push(defaultPage())
           idx = (@project.pages.length - 1)
           run = () =>
                goToPage(idx)
                refreshUI()
           setTimeout(run, 20)
           run2 = () =>
                @calculatePage(idx)
           setTimeout(run2, 30)


        @deletePage = (index) =>
            total = @project.pages.length
            if total > 1
                array = @project.pages
                @project.pages = (x for x in array when x != array[index])

                run = () ->
                    if total - 1 == index
                        goToPage(index - 1)
                    else
                        goToPage(index)
                setTimeout(run, 20)

        @checkLineWidth = (text) => ( /^\d*$/.test(text) &&  parseInt(text) >= 0 )

        @checkPointSize = (text) => ( /^\d*(\.\d*)?$/.test(text) && parseFloat(text) >= 0 )

        @matchedNatural = (text, from, to) => ( /^\d*$/.test(text) &&  parseInt(text) >= from && parseInt(text) <= to )

        @matchedReal    = (text, from, to) => ( /^\d*(\.\d*)?$/.test(text) && parseFloat(text) >= from && parseFloat(text) <= to )

        @isPlot2u = (grId, plID) =>
            grType = @graphs[grId].graphType
            plType = @graphs[grId].plots[plID].plotType
            (grType == "2D" && (plType == "lines" || plType == "points" || plType == "linespoints" || plType == "filledcurve"))

        @isPlot3u = (grId, plID) =>
            grType = @graphs[grId].graphType
            plType = @graphs[grId].plots[plID].plotType
            (grType == "3D" || (plType == "xerrorbars" || plType == "yerrorbars"))

        @isPlot4u = (grId, plID) =>
            grType = @graphs[grId].graphType
            plType = @graphs[grId].plots[plID].plotType
            (plType == "xyerrorbars")

        @changeUsingWarning = (usingType) =>
            if usingType == 1
                @usingTipDescr = 'format: `a:b` ,a - column with x cords, b - column with y cords'
            else if usingType == 2
                @usingTipDescr = 'format: `a:b:c`, a - column with x cords, b - column with y cords, c - column with z cords'
            else if usingType == 3
                @usingTipDescr = 'format: `a:b:c` ,a - column with x cords, b - column with y cords, c - column with errors'
            else if usingType == 4
                @usingTipDescr = 'format: `a:b:c:d` ,a - column with x cords, b - column with y cords, c - column with x errors, d - column with y errors'

        @checkUsing = (idParent, idPlot) =>
            if @isPlot2u(idParent, idPlot)
                return !@matchedUsing2(@graphs[idParent].plots[idPlot].using)
            else if @isPlot3u(idParent, idPlot) && @graphs[idParent].graphType == '3D'
                return !@matchedUsing3(@graphs[idParent].plots[idPlot].using)
            else if @isPlot3u(idParent, idPlot) && @graphs[idParent].graphType == '2D'
                return !@matchedUsing3(@graphs[idParent].plots[idPlot].using)
            else
                return !@matchedUsing4(@graphs[idParent].plots[idPlot].using)

        @matchedUsing2 = (text) => /^(\d+:){1}\d$/.test(text)

        @matchedUsing3 = (text) => /^(\d+:){2}\d$/.test(text)

        @matchedUsing4 = (text) => /^(\d+:){3}\d$/.test(text)

        @notEmpty = (text)  => text != ""

        @confirmDataChange = () => @graphs[@modalGraph].plots[@modalPlot].dataSet = $('#plot-data-set-modal').val()

        @drawGraph = (index) =>
            @requestGraph(index)
                .then(
                    (data) =>
                        img = new Image()
                        canvas = $("#plot-svg-" + index)[0]
                        ctx = canvas.getContext('2d')
                        xml = data
                        ctx.clearRect(0, 0, canvas.width, canvas.height)

                        f = () => ctx.drawImage(img,0,0)
                        img.addEventListener("load", f, false)
                        utf8_to_b64 = `function b64EncodeUnicode(str) {
                            return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, function(match, p1) {
                                return String.fromCharCode('0x' + p1);
                            }));
                        }`

                        img.src  = 'data:image/svg+xml;base64,' + utf8_to_b64(xml)
                    ,
                    (error) =>
                        alert "Unable to create graph" + "\n\n"
                    )

        @resizeCanvas = (width, height, id) ->
            canvas = $('#plot-svg-' + id)
            canvas.attr('width', width)
            canvas.attr('height', height)

        @requestGraph = (id) =>
            deferred = $q.defer()
            tempProject = defaultProject([@graphs[id]])
            tempPage = defaultPage()
            tempGraphRef = graphRef(0, 0.0, 0.0, 1.0, 1.0)
            tempPage.graphRefs.push(tempGraphRef)

            canvas = $('#plot-svg-' + id)
            container = $(canvas).parent()
            width =  container.width() - 20
            height = container.height() - 20

            tempPage.width = '' + width
            tempPage.height = '' + height
            tempProject.pages = [tempPage]

            @resizeCanvas(width, height, id)
            $http.post('/graph', tempProject)
                 .success((data, status, headers) =>
                                deferred.resolve(data))
                 .error((data, status, headers) =>
                                deferred.reject(data))
            deferred.promise

        @isAnyProjectLoaded = () => @project != undefined

        @refreshFiles = (data, status) =>
            @files = data
            $scope.$apply()

        @validProjectName = () =>
            names = ( x for x in @projectSelectList when x == @newProjectName)
            return !(names.length != 0 || @newProjectName == '')

        @confirmProjectName = () =>
            proj = defaultProject([defaultGraph()])
            proj.name = @newProjectName
            @projectSelectList.push(proj.name)
            @projectList .push(proj)
            @selectedOption = proj.name
            @changeProject(@selectedOption)
            return true

        @newProject = () => @newProjectName = "untitled"

        removeProjectCallback = (data, status) =>
            if data.status == "deleted" || data.status == "noProject"
                alert("Project deleted!")
                projectName = @project.name
                @projectList = (x for x in  @projectList when x.name != projectName)
                @projectSelectList = (x for x in @projectSelectList when x != projectName)

                @project = undefined
                @graphs = undefined
                $scope.$apply()
            else
                alert("Cannot delete project!")


        @removeProject = () => $.ajax '/project' + '?' + $.param({projectName: @project.name}),
                                    type: 'DELETE'
                                    dataType: 'html'
                                    success: removeProjectCallback


        ##$.get '/project', { projectName: @project.name }, removeProjectCallback

        @changeProject = (projectName) =>
            list = @projectList
            if list != null && list.length > 0
                selectedProject = (x for x in list when x.name == projectName)[0]
                @project = selectedProject
                @graphs = @project.graphs
                @selectedPage = 'Page 1'
                refreshUI()
                $scope.$apply()
                $('#plot-pagination li').first().addClass('active')
                $('#page-pagination li').first().addClass('active')

        @changeProjectHandler = () => @changeProject(@selectedOption)

        @refreshProjects = (data, status) =>
            @projectList = data
            @projectSelectList = (x.name for x in data)
            @selectedOption = @projectSelectList[0]                                
            @changeProject(@selectedOption)

        requestProjectGen = () =>
            deferred = $q.defer()
            $http.post('/generateProject', @project)
                             .success((data, status, headers) =>
                                            deferred.resolve(data))
                             .error((data, status, headers) =>
                                            deferred.reject(data))
            deferred.promise

        str2bytes = (str) =>
            bytes = new Uint8Array(str.length)
            for i in [0..(str.length - 1)]
                do () ->
                    bytes[i] = str.charCodeAt(i)
            bytes


        @downloadFile = (f) =>
            ref = '/generatedProject'
            $('#download').attr({target: '_blank', href  : ref})
            $('#download')[0].click()

        @generateProject = () =>
            requestProjectGen().then(
                (data) => @downloadFile(), 
                (error) => alert "Unable to create project" + "\n\n"
                )

        requestProjectSave = () =>
            deferred = $q.defer()
            $http.post('/saveProject', @project)
                             .success((data, status, headers) =>
                                            deferred.resolve(data))
                             .error((data, status, headers) =>
                                            deferred.reject(data))
            deferred.promise

        @saveProject = () =>
            requestProjectSave().then((data) => alert "Project saved!", (error) => alert "Unable to save project" + "\n\n")

        @clearGraphRefs = (index) =>
            draggables = $('.draggable' + index)
            originPos = $('#origin' + index).offset()
            originX = originPos.left
            originY = originPos.top
            for d in draggables
                do () =>
                    @project.pages[index].graphRefs = []
                    d.removeAttribute('drop-w')
                    d.removeAttribute('drop-h')
                    d.removeAttribute('drop-x')
                    d.removeAttribute('drop-y')
                    d.style.width = 50 + 'px'
                    d.style.height = 50 + 'px'
                    x = 0
                    y = 0
                    d.setAttribute('data-x', x)
                    d.setAttribute('data-y', y)
                    d.style.webkitTransform = d.style.transform = 'translate(' + x + 'px,' + y + 'px)'

        @defaultLayout = () =>
           id = 0
           for gr in @graphs
                do (gr) =>
                    p = defaultPage()
                    p.graphRefs.push(graphRef(id, 0.0, 0.0, 1.0, 1.0))
                    @project.pages.push(p)
                    id += 1

           run = () =>
                 refreshUI()
           setTimeout(run, 20)

        @calculatePageEvent = (index) =>
            run2 = () => @calculatePage(index)
            setTimeout(run2, 500)

        @calculatePage = (index) =>
            p = @project.pages[index]
            height = p.height
            width = p.width
            ratio = height/width
            pageIn = $('#page-in-' + index)

            container = document.getElementById('page-' + index)
            containerHeight = parseInt(container.offsetHeight)
            containerWidth = parseInt(container.offsetWidth)

            usedHeight = 0
            usedWidth = 0
            base = 0

            if containerHeight > containerWidth
                base = containerWidth
            else base = containerHeight

            if ratio < 1
                usedHeight = Math.floor(base*ratio)
                usedWidth = Math.floor(base)
                pageIn.css('width', '' + usedWidth + 'px' )
                pageIn.css('height', '' + usedHeight + 'px' )
            else
                usedHeight = Math.floor(base)
                usedWidth = Math.floor(base/ratio)
                pageIn.css('width', '' + usedWidth + 'px' )
                pageIn.css('height', '' + usedHeight + 'px' )

            baseH = Math.ceil(usedHeight/10)
            baseW = Math.ceil(usedWidth/10)


            @pagePosition = pageIn.offset()
            position = @pagePosition

            prepareGraphsInPage(index, baseW, baseH, position.left, position.top, baseH, baseW, usedHeight, usedWidth)

            interact('#page-in-' + index).dropzone(
                accept: '.draggable'
                overlap: 0.75
                ondrop: (event) =>
                        draggableElement = event.relatedTarget
                        dropzoneElement = event.target
                        dropRect = interact.getElementRect(dropzoneElement)
                        dragRect = interact.getElementRect(draggableElement)
                        draggableElement.setAttribute('id-in-page', index)
                        indexGraph = parseInt(draggableElement.querySelector('p').textContent) - 1
                        posX = (dragRect.left - dropRect.left)/dropRect.width
                        posY = (dropRect.bottom - dragRect.bottom)/dropRect.height
                        widt = dragRect.width/dropRect.width
                        heig = dragRect.height/dropRect.height

                        refs = @project.pages[index].graphRefs
                        refsOk = (x for x in refs when x.graphId == indexGraph)

                        if refsOk.length == 0
                            draggableElement.setAttribute('drop-w', dropRect.width)
                            draggableElement.setAttribute('drop-h', dropRect.height)
                            draggableElement.setAttribute('drop-x', dropRect.left)
                            draggableElement.setAttribute('drop-y', dropRect.bottom)
                            ref = graphRef(indexGraph, posX, posY, widt, heig)
                            @project.pages[index].graphRefs.push(ref)
                        else
                            ref = refsOk[0]
                            ref.x = posX
                            ref.y = posY

                ondragleave: (event) =>
                        draggableElement = event.relatedTarget
                        draggableElement.removeAttribute('drop-w')
                        draggableElement.removeAttribute('drop-h')
                        draggableElement.removeAttribute('drop-x')
                        draggableElement.removeAttribute('drop-y')
                        indexGraph = parseInt(draggableElement.querySelector('p').textContent) - 1
                        array = @project.pages[index].graphRefs
                        @project.pages[index].graphRefs = (x for x in array when x.graphId != indexGraph))



        prepareGraphsInPage = (pageIdx, snapX, snapY, relX, relY, baseHeight, baseWidth, height, width) =>
            myTargets = []
            for i in [0..7]
                do (i) ->
                    factor = i/10
                    xFun = (x, y) ->
                        x: relX + Math.round(factor*width)
                        y: y
                        range: 40
                    yFun = (x, y) ->
                        x: x
                        y: relY + Math.round(factor*height)
                        range: 40
                    myTargets.push(xFun)
                    myTargets.push(yFun)

            draggables = $('.draggable' + pageIdx)

            originPos = $('#origin' + pageIdx).offset()

            originX = originPos.left
            originY = originPos.top


            refs = @project.pages[pageIdx].graphRefs
            for d in draggables
                do (d) =>
                    id = parseInt(d.querySelector('p').textContent) - 1
                    ref = (x for x in refs when x.graphId == id)
                    if ref.length != 0
                        d.setAttribute('drop-w', width)
                        d.setAttribute('drop-h', height)
                        d.setAttribute('drop-x', relX)
                        d.setAttribute('drop-y', relY)
                        d.style.width = ref[0].width*width + 'px'
                        d.style.height = ref[0].height*height + 'px'
                        x = relX - originX + (ref[0].x)*width
                        y = relY - originY - parseInt(d.style.height) + (1.0 - ref[0].y)*height
                        d.setAttribute('data-x', x)
                        d.setAttribute('data-y', y)
                        d.style.webkitTransform = d.style.transform = 'translate(' + x + 'px,' + y + 'px)'

            interact('.draggable' + pageIdx).draggable(
                        restrict:
                            restriction: "#layout-page"
                            endOnly: true
                            elementRect: { top: 0, left: 0, bottom: 1, right: 1 }
                        onmove: dragMoveListener
                        snap:
                            targets: myTargets
                            range: Infinity
                            relativePoints: [ { x: 0, y: 0 }, {x: 1, y: 1}, {x: 1, y: 0}, {x: 0, y: 1}, {x: 0.5, y: 0.5} ]
                        onend: (event) ->
                    ).resizable(
                        edges: { left: false, right: true, bottom: true, top: false }
                        restrict:
                            restriction: "#layout-page"
                        snap:
                            targets: myTargets
                            range: Infinity
                            relativePoints: [ { x: 0, y: 0 }, {x: 1, y: 1}, {x: 1, y: 0}, {x: 0, y: 1}, {x: 0.5, y: 0.5} ]
                    ).on('resizemove', resizeListener)
                    .on('resizeend', (event) =>
                        target = event.target
                        dragRect = interact.getElementRect(target)
                        dropW = target.getAttribute('drop-w')
                        dropH = target.getAttribute('drop-h')
                        dropX = target.getAttribute('drop-x')
                        dropY = target.getAttribute('drop-y')

                        if dropW != null and dropH != null
                            indexGraph = parseInt(target.querySelector('p').textContent) - 1
                            posY = (dropY - dragRect.bottom)/dropH
                            widt = dragRect.width/dropW
                            heig = dragRect.height/dropH
                            refs = @project.pages[pageIdx].graphRefs
                            ref = (x for x in refs when x.graphId == indexGraph)
                            ref[0].width = widt
                            ref[0].height = heig
                            ref[0].y = posY)

        dragMoveListener = (event) ->
            target = event.target
            x = (parseFloat(target.getAttribute('data-x')) || 0) + event.dx
            y = (parseFloat(target.getAttribute('data-y')) || 0) + event.dy
            target.style.webkitTransform = target.style.transform = 'translate(' + x + 'px, ' + y + 'px)'
            target.setAttribute('data-x', x)
            target.setAttribute('data-y', y)

        resizeListener = (event) ->
            target = event.target
            x = (parseFloat(target.getAttribute('data-x')) || 0)
            y = (parseFloat(target.getAttribute('data-y')) || 0)

            target.style.width  = event.rect.width + 'px'
            target.style.height = event.rect.height + 'px'

            ## translate when resizing from top or left edges
            x += event.deltaRect.left
            y += event.deltaRect.top

            target.style.webkitTransform = target.style.transform = 'translate(' + x + 'px,' + y + 'px)'
            target.setAttribute('data-x', x);
            target.setAttribute('data-y', y);

        @calculateAllPages = () =>
            run1 = () =>
                container = document.getElementById('page-' + 0)
                @containerH = parseInt(container.offsetHeight)
                @containerW = parseInt(container.offsetWidth)
            setTimeout(run1, 200)
            if @isCalculated == false
                run2 = () =>
                    for p in [0..(@project.pages.length-1)]
                        do (p) => @calculatePage(p)
                setTimeout(run2, 210)
                @isCalculated = true

        init()
        return


    angular
        .module('App')
        .controller('Plotter' , Plotter)
)()
