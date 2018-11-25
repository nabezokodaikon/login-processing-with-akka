console.log("header");

document.addEventListener("DOMContentLoaded", function(event) {
  console.log("DOM fully loaded and parsed");

  var newDiv = document.createElement("div"); 
  // いくつかの内容を与えます 
  var newContent = document.createTextNode("Hi there and greetings!"); 
  // テキストノードを新規作成した div に追加します
  newDiv.appendChild(newContent);  

  const bodyNode = document.getElementsByTagName('body').item(0);
  const referenceNode = document.getElementsByTagName("h1").item(0);
  bodyNode.insertBefore(newDiv, referenceNode);

});
