const API = "https://backend-ysf3.onrender.com";
async function test() {
  const res = await fetch(`${API}/health`);
  console.log("Health:", await res.json());
}
test();
