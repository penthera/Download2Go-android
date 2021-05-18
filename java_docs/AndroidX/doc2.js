/*
 Copyright 2009 by Alan Snyder.  All Rights Reserved.
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

 This code is free software; you can redistribute it and/or modify it
 under the terms of the GNU General Public License version 2 only, as
 published by the Free Software Foundation.

 This code is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 version 2 for more details (a copy is included in the LICENSE file that
 accompanied this code).

 You should have received a copy of the GNU General Public License version
 2 along with this work; if not, write to the Free Software Foundation,
 Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
*/

var isFirefox = navigator.userAgent.indexOf("Firefox") >= 0;
var isSafari = navigator.vendor && navigator.vendor.indexOf("Apple") >= 0;

var classSelectorHTML;
var selectedNode;
var selectedNodeWasAutomaticallyExpanded;
var showingInheritedMembers = true;
var showingDeprecated = false;

var accessViewLevel = 0;	// values are 0, 1, 2, 3 corresponding to "client", "subclass", "package", "implementation"

var accessViewSelectors = new Array();
var availableNodesAtLevel = new Array(0, 0, 0, 0);

var entityKinds = new Array();

var fieldSelectorLinks = new Array();
var enumConstantSelectorLinks = new Array();
var classSelectorLinks = new Array();
var constructorSelectorLinks = new Array();
var methodSelectorLinks = new Array();
var staticMethodSelectorLinks = new Array();
var annotationTypeRequiredElementSelectorLinks = new Array();
var annotationTypeOptionalElementSelectorLinks = new Array();
var propertySelectorLinks = new Array();

function findObject(id)
{
	if (document.getElementById) {
		return document.getElementById(id);
	}

	if (document.all) {
		return document.all[id];
	}

	return null;
}

function getChildrenByTagName(node, tagName)
{
	var result = new Array();
	if (node.childNodes) {
		var nodeCount = node.childNodes.length;
		for (var i = 0; i < nodeCount; i++) {
			var child = node.childNodes[i];
			if (child.tagName && child.tagName.toLowerCase() == tagName) {
				result[result.length] = child;
			}
		}
	}

	return result;
}

function splitClassNames(s)
{
	if (s == undefined) {
		return null;
	}

	return s.split(/ +/);
}

function nodeClassNames(n)
{
	return splitClassNames(n.className);
}

function nodeHasClassName(n, s)
{
	var classNames = nodeClassNames(n);

	if (classNames == null) {
		return false;
	}

	for (var i = 0; i < classNames.length; i++) {
		if (classNames[i] == s) {
			return true;
		}
	}

	return false;
}

function getChildrenByTagNameAndClassName(node, tagName, className)
{
	var result = new Array();
	if (node.childNodes) {
		var nodeCount = node.childNodes.length;
		for (var i = 0; i < nodeCount; i++) {
			var child = node.childNodes[i];
			if (child.tagName && child.tagName.toLowerCase() == tagName && nodeHasClassName(child, className)) {
				result[result.length] = child;
			}
		}
	}

	return result;
}

function getElementsByTagNameAndClassName(node, tagName, className)
{
	var result = new Array();

	if (node.childNodes) {
		var nodes = node.getElementsByTagName(tagName);
		if (nodes) {
			for (var i = 0; i < nodes.length; i++) {
				var child = nodes[i];
				if (nodeHasClassName(child, className)) {
					result[result.length] = child;
				}
			}
		}
	}
	return result;
}

function setHidden(node, isHidden)
{
	addRemoveStyle(node, isHidden, "hidden");
}

function setInvisible(node, isHidden)
{
	addRemoveStyle(node, isHidden, "invisible");
}

function setSelected(node, isSelected)
{
	if (node.id) {
		// alert("Set selected: " + node.id + " " + isSelected);
	}

	addRemoveStyle(node, isSelected, "selected");
}

function setSelectedMember(node, isSelected)
{
	if (node.id) {
		// alert("Set selected member: " + node.id + " " + isSelected);
	}

	addRemoveStyle(node, isSelected, "selectedMember");
}

function setDisabled(node, isDisabled)
{
	// alert("Set disabled: " + node.id + " " + isDisabled);
	addRemoveStyle(node, isDisabled, "disabled");
}

function addRemoveStyle(node, isAdd, styleName)
{
	if (node == null) {
		return;
	}

	var classNames = node.className != null ? node.className.split(" ") : new Array();

	if (isAdd) {
		for (var i = 0; i < classNames.length; i++) {
			if (classNames[i] == styleName) {
				return;
			}
		}
		node.className = node.className + " " + styleName;
		return;
	}

	var index = -1;

	for (var i = 0; i < classNames.length; i++) {
		if (classNames[i] == styleName) {
			index = i;
			break;
		}
	}

	if (index < 0) {
		return;
	}

	var s = "";
	for (var i = 0; i < classNames.length; i++) {
		if (i != index) {
			s = s + " " + classNames[i];
		}
	}
	node.className = s.substring(1);
}

function getPackageFileURL(pkg, filename)
{
	if (typeof(currentPackage) != "undefined" && currentPackage === pkg) {
        return filename;
    }

	if (pathSeparator) {
		path = pkg.replace(/\./g, pathSeparator);
		var rp = rootPath;
		if (rp.length == 0) {
			rp = path;
		} else {
			rp = rootPath + path;
		}
		var url = rp + pathSeparator + filename;
		return url;
	}
}

function setViewOptionCookie(name, value)
{
	var expires = new Date(2200, 1, 1, 1, 1);
	var path = "/";
	document.cookie = name + "=" + escape(value) + "; expires=" + expires.toGMTString() + "; path=" + path;
}

function getCookie(name)
{
	var pattern = name + "=";
	var ss = document.cookie.split(/ *; */);
	for (var i = 0; i < ss.length; i++) {
		var s = ss[i];
		if (s.indexOf(pattern) == 0) {
			return s.substring(pattern.length);
		}
	}
}

function setInheritedVisible(show)
{
	showingInheritedMembers = show;
	setViewOptionCookie("showInheritedMembers", show ? "true" : "false");
	updateInheritedMembersControls();
	scanSummaries();
}

function updateInheritedMembersControls()
{
	var showControl = findObject("showInheritedMembersSelector");
	var hideControl = findObject("hideInheritedMembersSelector");
	updateToggleControl(showControl, showingInheritedMembers);
	updateToggleControl(hideControl, !showingInheritedMembers);
}

function setDeprecatedVisible(show)
{
	showingDeprecated = show;
	setViewOptionCookie("showDeprecated", show ? "true" : "false");
	updateDeprecatedControls();
	scanSummaries();
}

function updateDeprecatedControls()
{
	var showControl = findObject("showDeprecatedSelector");
	var hideControl = findObject("hideDeprecatedSelector");
	updateToggleControl(showControl, showingDeprecated);
	updateToggleControl(hideControl, !showingDeprecated);
}

function updateToggleControl(node, enable)
{
	if (node) {
		setSelected(node, enable);
	}
}

function overItemName(node, show)
{
	var member = findMemberNodeFromContainedElement(node);
	if (member == null) {
		return;
	}

	if (descriptionIsExpanded(member)) {
		return;
	}

	var signatureNode = getElementsByTagNameAndClassName(member, "div", "rollover")[0];
	if (signatureNode) {
		setHidden(signatureNode, !show);
	}
}

function descriptionIsExpanded(member)
{
	var overviewNode = getElementsByTagNameAndClassName(member, "div", "descriptionOverview")[0];
	if (overviewNode) {
		return nodeHasClassName(overviewNode, "hidden");
	} else {
		return false;
	}
}

function toggleFullDescription(node)
{
	var member = findMemberNodeFromContainedElement(node);
	if (member == null) {
		return;
	}

	var images = getChildrenByTagName(node, "img");
	var revealImage = images[0];
	var hideImage = images[1];
	var overviewNode = getElementsByTagNameAndClassName(member, "div", "descriptionOverview")[0];
	var detailNode = getElementsByTagNameAndClassName(member, "div", "descriptionDetail")[0];

	if (overviewNode && detailNode) {
		if (nodeHasClassName(overviewNode, "hidden")) {
			setHidden(overviewNode, false);
			setHidden(detailNode, true);
			//overviewNode.style.display="block";
			//detailNode.style.display="none";
			revealImage.style.display="inline";
			hideImage.style.display="none";
		} else {
			setHidden(overviewNode, true);
			setHidden(detailNode, false);
			//overviewNode.style.display="none";
			//detailNode.style.display="block";
			revealImage.style.display="none";
			hideImage.style.display="inline";
		}
	}
}

function ensureNodeExpanded(member)
{
	var revealImage = getElementsByTagNameAndClassName(member, "img", "revealDetail")[0];
	var hideImage = getElementsByTagNameAndClassName(member, "img", "hideDetail")[0];
	var overviewNode = getElementsByTagNameAndClassName(member, "div", "descriptionOverview")[0];
	var detailNode = getElementsByTagNameAndClassName(member, "div", "descriptionDetail")[0];

	if (overviewNode && detailNode) {
		setHidden(overviewNode, true);
		setHidden(detailNode, false);
		//overviewNode.style.display="none";
		//detailNode.style.display="block";
		revealImage.style.display="none";
		hideImage.style.display="inline";
	}
}

function ensureNodeNotExpanded(member)
{
	var revealImage = getElementsByTagNameAndClassName(member, "img", "revealDetail")[0];
	var hideImage = getElementsByTagNameAndClassName(member, "img", "hideDetail")[0];
	var overviewNode = getElementsByTagNameAndClassName(member, "div", "descriptionOverview")[0];
	var detailNode = getElementsByTagNameAndClassName(member, "div", "descriptionDetail")[0];

	if (overviewNode && detailNode) {
		setHidden(overviewNode, false);
		setHidden(detailNode, true);
		//overviewNode.style.display="block";
		//detailNode.style.display="none";
		revealImage.style.display="inline";
		hideImage.style.display="none";
	}
}

function updateAccessViewSelectors()
{
	for (var i = 0; i < 4; i++) {
		updateAccessViewSelector(i);
	}
}

function updateAccessViewSelector(level)
{
	var selector = accessViewSelectors[level];
	updateToggleControl(selector, accessViewLevel == level);
}

function setAccessViewLevel(level)
{
	accessViewLevel = level;
	setViewOptionCookie("accessViewLevel", "v" + level);
	updateAccessViewSelectors();
	scanSummaries();
}

function initialViewOptions()
{
	accessViewSelectors[0] = findObject("clientViewSelector");
	accessViewSelectors[1] = findObject("subclassViewSelector");
	accessViewSelectors[2] = findObject("packageViewSelector");
	accessViewSelectors[3] = findObject("implementationViewSelector");

	var accessView = getCookie("accessViewLevel");
	if (accessView == "v1") {
		accessViewLevel = 1;
	} else if (accessView == "v2") {
		accessViewLevel = 2;
	} else if (accessView == "v3") {
		accessViewLevel = 3;
	} else {
		accessViewLevel = 0;
	}
	updateAccessViewSelectors();

	var showInh = getCookie("showInheritedMembers");
	if (showInh == "false") {
		showingInheritedMembers = false;
	} else {
		showingInheritedMembers = true;
	}
	updateInheritedMembersControls();

	var showDep = getCookie("showDeprecated");
	if (showDep == "false") {
		showingDeprecated = false;
	} else {
		showingDeprecated = true;
	}
	updateDeprecatedControls();
}

function showClientView()
{
	setAccessViewLevel(0);
}

function showSubclassView()
{
	setAccessViewLevel(1);
}

function showPackageView()
{
	setAccessViewLevel(2);
}

function showImplementationView()
{
	setAccessViewLevel(3);
}

function nodeAccessViewLevel(node)
{
	if (nodeHasClassName(node, "implementation")) {
		return 3;
	} else if (nodeHasClassName(node, "package")) {
		return 2;
	} else if (nodeHasClassName(node, "subclass")) {
		return 1;
	} else {
		return 0;
	}
}

function summaryKind(summary)
{
	var classNames = nodeClassNames(summary);

    if (classNames == null) {
        return undefined;
    }

	for (var i = 0; i < classNames.length; i++) {
	    var name = classNames[i];
	    var pos = name.indexOf("ElementList");
	    if (pos > 0) {
	        return name.substring(0, pos);
		}
	}

	return undefined;
}

function goToLocalLink(anchorName)
{
	var node = findMemberByAnchorName(anchorName);
    if (node != null) {
        selectMember(node);
        scrollToView(node);

        // TODO: set the default index of the appropriate selector

        //var container = findObject("dataArea");
        //if (container) {
        //	container.scrollTop = node.offsetTop;
        //	return false;
        //}

        return true;
    }

    node = findAnchor(anchorName);
	if (node != null) {
	    scrollToView(node);
		return true;
	}

    return false;
}

function scrollToView(node)
{
	var scrollingContainer = findObject("dataArea");
	if (!scrollingContainer) {
		return;
	}

 	var offset = node.offsetTop;
 	var parent = node.offsetParent;

 	while (parent != scrollingContainer) {
 		if (parent == null) {
 			return;
 		}
 		offset += parent.offsetTop;
 		parent = parent.offsetParent;
 	}

 	//alert("Offset is: " + offset);
 	scrollingContainer.scrollTop = offset;2
}

function elementTop(node)
{
	var container = findObject("dataArea");
	if (container) {
  		var pos = 0;
  		for (;;) {
  			pos += node.offsetTop;
  			node = node.offsetParent;
  			if (node == null || node == container) {
  				break;
  			}
  		}
  	}
	return pos;
}

function isMemberNode(e)
{
	return nodeHasClassName(e, "member");
}

function findMemberNodeFromContainedElement(e)
{
	while (e != null) {
		if (isMemberNode(e)) {
			return e;
		}
		e = e.parentNode;
	}
	return null;
}

function findMemberByAnchorName(anchorName)
{
    var anchor = findAnchor(anchorName);
    if (anchor != null) {
        var node = findMemberNodeFromContainedElement(anchor);
        if (node != null) {
            return node;
        }
    }
	return null;
}

function findAnchor(anchorName)
{
	var anchors = document.anchors;
	for (var i = 0; i < anchors.length; i++) {
		var anchor = anchors[i];
		if (anchor.name == anchorName) {
			return anchor;
		}
	}
	return null;
}

function selectMember(node)
{
	if (node == selectedNode) {
		return;
	}

	if (selectedNode != null) {
		setSelectedMember(selectedNode, false);
		if (selectedNodeWasAutomaticallyExpanded) {
			ensureNodeNotExpanded(selectedNode);
		}
	}

	selectedNode = node;

	setSelectedMember(node, true);

	if (descriptionIsExpanded(node)) {
		selectedNodeWasAutomaticallyExpanded = false;
	} else {
		ensureNodeExpanded(node);
		selectedNodeWasAutomaticallyExpanded = true;
	}
}

function getItemTitle(node)
{
	// TODO: may want to remove package prefixes from parameter types

	var anchor = getElementsByTagNameAndClassName(node, "a", "memberAnchor")[0];
	if (anchor) {
		return anchor.name;
	}

	var sig = getElementsByTagNameAndClassName(node, "div", "signature")[0];
	if (sig) {
		var titles = getElementsByTagNameAndClassName(sig, "span", "itemTitle");
		if (titles.length == 1) {
			return titles[0].innerHTML;
		}
		titles = getElementsByTagNameAndClassName(sig, "a", "itemTitle");
		if (titles.length == 1) {
			return titles[0].innerHTML;
		}
	}
	return "Item";
}

function getOptions(selector)
{
	if (selector.options.tagName == "SELECT") {	// for IE7
		return selector.getElementsByTagName("option");
	} else {
		return selector.options;
	}
}

function goToSelection(selector, links)
{
	var index = selector.selectedIndex;
	if (index == 0) {
		return;
	}

	resetSelector(selector);

	if (index == 0) {
	    var options = getOptions(selector);
		var href = options[index].value;
		window.location.replace(href);
		return;
	}

	if (index > 0 && index <= links.length) {
		var node = links[index-1];

		selectMember(node);
		scrollToView(node);

		selector.defaultInitialSelectedIndex = index;
	}
}

function selectPackage(selectorIndex)
{
	var selector = findObject("packageSelector");
	var options = selector.getElementsByTagName("option");

	if (selectorIndex > 0 && selectorIndex < options.length) {
		var option = options[selectorIndex];
		var name = option.innerHTML;

		if (window.currentPackage != undefined && name == currentPackage) {
			selector.defaultInitialSelectedIndex = selectorIndex;
			resetSelector(selector);
			return;
		}

		viewPackage(name);
	}
}

function selectClass(selector)
{
	var index = selector.selectedIndex;
	if (index == 0) {
		return;
	}

	var options = getOptions(selector);
	var name = options[index].value;

	if (name.length == 0) {
		return false;
	}

	var pos = name.indexOf("/");
	var pkg = name.substring(0, pos);
	var className = name.substring(pos+1);

	if (window.currentClass != undefined && className == currentClass) {
		return false;
	}

	var url = getPackageFileURL(pkg, className + ".html");
	if (url) {
		window.location = url;
	}
	return false;
}

function initializeSelector(selector)
{
	// Some browsers (Firefox) receive mouse down events when the user scrolls
	// the list or makes a selection. Therefore, we change the selection
	// only when the current selection is index zero.

	if (selector.defaultInitialSelectedIndex && selector.selectedIndex == 0) {
		var index = selector.defaultInitialSelectedIndex;

		// changing the DOM in Firefox causes problems using the menu when scrolling
		if (isFirefox) {
			selector.selectedIndex = index;
			//debugOut("Firefox: changed selected option to " + index);
			return;
		}

		try {
	        var options = getOptions(selector);
			options[0].defaultSelected = false;
			options[index].defaultSelected = true;
			//debugOut("Changed default selected option to " + index);
		} catch (e) {
			selector.selectedIndex = index;
			//debugOut("Changed selected option to " + index);
		}
	}
}

function resetSelector(selector)
{
	try {
	    var options = getOptions(selector);
		options[0].defaultSelected = true;
		for (var i = 1; i < options.length; i++) {
			options[i].defaultSelected = false;
		}
	} catch (e) {
	}
	selector.selectedIndex = 0;
}

function setupPackageSelector()
{
	var selector = findObject("packageSelector");
	if (selector) {
	    var options = getOptions(selector);

		if (window.currentPackage) {
			setSelectorIndex(selector, currentPackage);
		}

		var selectFunction = function() {
			selectPackage(selector.selectedIndex);
		};

		setupSelector(selector, selectFunction);
	}
}

function loadClassSelector()
{
    var selector = findObject("packageClassSelector");
	if (selector) {
        setInvisible(selector, true);
        if (window.classSelectorHTML) {

			if (selector.options.tagName == "SELECT") {	// IE7
				selector.outerHTML = '<select id="packageClassSelector">' + classSelectorHTML + '</option>';
				selector = findObject("packageClassSelector");
			} else {
	        	selector.innerHTML = classSelectorHTML;
			}

	        var options = getOptions(selector);
		    setInvisible(selector, options.length < 2);

		    if (window.currentClass) {
                setSelectorIndex(selector, currentClass);
		    }

            var selectFunction = function() {
                selectClass(selector);
            };

            setupSelector(selector, selectFunction);
	    }
    }
}

function setSelectorIndex(selector, targetValue)
{
	var options = getOptions(selector);

    for (var i = 0; i < options.length; i++) {
        if (options[i].innerHTML == targetValue) {
            selector.defaultInitialSelectedIndex = options[i].index;
            return;
        }
    }
}

function setupSelector(selector, selectFunction)
{
	selector.onmousedown = function() { initializeSelector(selector); };

	// Can use onclick for Safari, because Safari does not generate spurious events.
	// Still need the onchange/onblur combination for situations where a change
	// event is not generated.

	if (isSafari) {
		selector.onclick = selectFunction;
	}

	selector.onchange = selectFunction;
	selector.onblur = function() { resetSelector(selector); };	// not perfect
}

function getEntitySelectorKinds()
{
	var kinds = new Array();
	var selects = document.getElementsByTagName("select");

	for (var i = 0; i < selects.length; i++) {
		var select = selects[i];
		var id = select.id;
		var pos = id.indexOf("Selector");
		if (pos > 0) {
			var kind = id.substring(0, pos);
			try {
				var links = eval(kind + "SelectorLinks");
				if (links) {
					kinds[kinds.length] = kind;
				}
			} catch (e) {
			}
		}
	}

	return kinds;
}

function fillEntitySelector(kind)
{
	var selectorID = kind + "Selector";
	var selector = findObject(selectorID);

	if (selector == null) {
		return;
	}

	while (selector.length > 1) {
    	selector.remove(1);
	}

	var links = sortSelectorLinks(eval(kind + "SelectorLinks"));
	for (var i = 0; i < links.length; i++) {
		var opt = document.createElement("option");
		var title = getItemTitle(links[i]);
		opt.setAttribute("value", i);
		opt.innerHTML=title;
		selector.appendChild(opt);
	}

	selector.disabled = (links.length == 0);
}

function setupEntitySelectors()
{
	setupEntitySelector("class");
	setupEntitySelector("enumConstant");
	setupEntitySelector("field");
	setupEntitySelector("staticMethod");
	setupEntitySelector("constructor");
	setupEntitySelector("method");
	setupEntitySelector("property");
	setupEntitySelector("annotationTypeRequiredElement");
	setupEntitySelector("annotationTypeOptionalElement");
}

function setupEntitySelector(kind)
{
	var selectorID = kind + "Selector";
	var selector = findObject(selectorID);

	if (selector == null) {
		return;
	}

	var links = eval(kind + "SelectorLinks");

	var selectFunction = function() {
		goToSelection(selector, links);
	};

	setupSelector(selector, selectFunction);
}

function viewPackage(name)
{
	var url = getPackageFileURL(name, "package-summary.html");
	if (url) {
		window.location.replace(url);
	}
}

function isVisibleMember(node)
{
	if (node.className.indexOf(" inherited") > 0 && !showingInheritedMembers) {
		return false;
	}

	if (node.className.indexOf(" deprecated") > 0 && !showingDeprecated) {
		return false;
	}

	var level = nodeAccessViewLevel(node);
	availableNodesAtLevel[level]++;

	if (level > accessViewLevel) {
		return false;
	}

	return true;
}

function sortSelectorLinks(items)
{
	items.sort(function(a, b) {
		var n1 = getItemTitle(a);
		var n2 = getItemTitle(b);
			return n1 == n2
				? 0
				: (n1 > n2 ? 1 : -1);
			});
	return items;
}

function scanSummaries()
{
	var body = document.getElementsByTagName("body")[0];
    if (body == null) {
        return;
    }

    var isNodes = getElementsByTagNameAndClassName(body, "li", "inheritedMemberSummary");

    var foo = 0;

    for (var i = 0; i < isNodes.length; i++) {
        var isNode = isNodes[i];
        setHidden(isNode, !showingInheritedMembers);
    }

	for (var level = 1; level < 4; level++) {
		availableNodesAtLevel[level] = 0;
	}

	for (var i = 0; i < entityKinds.length; i++) {
		var kind = entityKinds[i];
		var links = eval(kind + "SelectorLinks");
		links.length = 0;
	}

	var nodes = getElementsByTagNameAndClassName(body, "table", "memberSummary");

	for (var i = 0; i < nodes.length; i++) {
		var node = nodes[i];
		scanSummary(node);
	}

	for (var level = 1; level < 4; level++) {
		var selector = accessViewSelectors[level];
		setDisabled(selector, availableNodesAtLevel[level] == 0);
	}

	for (var i = 0; i < entityKinds.length; i++) {
		var kind = entityKinds[i];
		fillEntitySelector(kind);
	}

	updateNavBoundary();
}

function scanSummary(summary)
{
	var kind = summaryKind(summary);
	if (!kind) {
		return;
	}

	var links = eval(kind + "SelectorLinks");
	var hidden = true;

	var nodes = getElementsByTagNameAndClassName(summary, "tr", "member");

	for (var i = 0; i < nodes.length; i++) {
		var node = nodes[i];
		if (isVisibleMember(node)) {
			links[links.length] = node;
			hidden = false;
			setHidden(node, false);
		} else {
			setHidden(node, true);
		}
		setSelectedMember(node, node == selectedNode);
	}

	setHidden(summary, hidden);
}

var documentWidthForIEBug;
var documentHeightForIEBug;

function windowResizeHandler()
{
	// If the window size changes, the navigation area may get shorter or taller

	var width = getViewportWidth();
	var height = getViewportHeight();

	if (documentWidthForIEBug != width || documentHeightForIEBug != height) {
		documentWidthForIEBug = width;
		documentHeightForIEBug = height;
		updateNavBoundary();
	}
}

// Track changes to the navigation area height

function updateNavBoundary()
{
	var navBar = findObject("topNavBar");
	var dataArea = findObject("dataArea");
	if (navBar && dataArea) {
		var fullHeight = document.body.offsetHeight;
		var topHeight = navBar.offsetHeight;
		var bottomHeight = fullHeight - topHeight;
		dataArea.style.height = bottomHeight + "px";
	}
}

function getViewportWidth()
{
	if (window.innerWidth) {
		return window.innerWidth;
	}
	if (document.documentElement
				&& document.documentElement.clientWidth
				&& document.documentElement.clientWidth != 0) {
		return document.documentElement.clientWidth;
	}

	return document.getElementsByTagName('body')[0].clientWidth;
}

function getViewportHeight()
{
	if (window.innerHeight) {
		return window.innerHeight;
	}
	if (document.documentElement
				&& document.documentElement.clientHeight
				&& document.documentElement.clientHeight != 0) {
		return document.documentElement.clientHeight;
	}

	return document.getElementsByTagName('body')[0].clientHeight;
}

var oArea = null;

function debugOut(s)
{
	if (!oArea) {
		oArea = document.createElement('textarea');
		oArea.rows=80;
		oArea.cols=80;
		oArea.value="";
		var div = findObject("dataArea");
		if (div) {
			div.appendChild(oArea);
		}
	}

	oArea.value = oArea.value + "\n" + s;
}

function fixNavBarLayout(nav)
{
	var nodes = getElementsByTagNameAndClassName(nav, "div", "pageTitle");
	if (nodes.length == 1) {
	    var outer = nodes[0];
	    nodes = outer.getElementsByTagName("span");
	    if (nodes.length > 0) {
	        var inner = nodes[0];
	        var outerHeight = outer.offsetHeight;
	        var innerHeight = inner.offsetHeight;
	        inner.style.position = "relative";
	        inner.style.top = ((outerHeight - innerHeight) / 2 - 1) + "px";
	    }
	}

	var nodes = getElementsByTagNameAndClassName(nav, "div", "userTitle");
	if (nodes.length == 1) {
	    var outer = nodes[0];
	    nodes = outer.getElementsByTagName("em");
	    if (nodes.length > 0) {
	        var inner = nodes[0];
	        var outerHeight = outer.offsetHeight;
	        var innerHeight = inner.offsetHeight;
	        inner.style.position = "relative";
	        inner.style.top = ((outerHeight - innerHeight) / 2 - 1) + "px";
	    }
	}
}

function initializePage()
{
	entityKinds = getEntitySelectorKinds();

	initialViewOptions();
	loadClassSelector();
	setupPackageSelector();
	setupEntitySelectors();
	scanSummaries();

	var nav = findObject("topNavBar");
	if (nav) {
		setHidden(nav, false);
        fixNavBarLayout(nav);
	}

	var div = findObject("dataArea");
	if (div) {
		setHidden(div, false);
	}

	var anchor = window.location.hash.replace('#', '');
	if (anchor.length > 0) {
		anchor = unescape(anchor);
		var node = findMemberByAnchorName(anchor);
		if (node != null) {
			selectMember(node);
			scrollToView(node);	// for Firefox
		} else {
			// alert("No member with anchor: " + anchor);
		}
	}

	window.onresize = windowResizeHandler;

	updateNavBoundary();

	// For FireFox:
	window.setTimeout("updateNavBoundary();", 100);
}

window.onload = initializePage;
