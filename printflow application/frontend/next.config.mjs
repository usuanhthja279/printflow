/** @type {import('next').NextConfig} */
const nextConfig={
  output:'standalone',
  experimental:{cpus:2,proxyTimeout:90000,proxyClientMaxBodySize:"22mb"},
  async rewrites(){
    const base = process.env.API_INTERNAL_URL || 'http://localhost:8080';
    return [{source:'/api/:path*',destination:`${base}/api/:path*`}];
  },
};
export default nextConfig;
