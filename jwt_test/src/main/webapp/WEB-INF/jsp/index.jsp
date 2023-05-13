<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
	index.jsp
	<form>
	<input name="username"/>
	</form>
	
	<c:out value="${loginUser}"/>

</body>
</html>