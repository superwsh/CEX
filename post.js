const fetch = require('node-fetch');

const fetchData = async () => {
  const response = await fetch('http://127.0.0.1:6001/uc/register/phone?phone=13800438029&password=12345678&username=hello0&country=%E4%B8%AD%E5%9B%BD&code=123456',
    {
        method: 'POST',
    }
  );
  const data = await response.json();
  console.log(data);
};

fetchData();