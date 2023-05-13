<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
	<title>Title</title>
	<script src="https://code.jquery.com/jquery-3.7.0.min.js" integrity="sha256-2Pmvv0kuTBOenSvLm6bvfBSSHrUJ+3A7x6P5Ebd07/g=" crossorigin="anonymous"></script>
</head>
<body>

index.jsp
<form>
<input name="username"/>
</form>

<div><c:out value="${loginUser}"/></div>
<div><c:out value="${expDate}"/></div>

<div><a href='<c:url value="/logout"/>'>/logout</a></div>

<h1></h1>

<script>
$(function(){
	var $h1 = $('h1:first');
	setInterval(function(){
		$h1.text(new Date());
	}, 100);
});
</script>

</body>
</html>