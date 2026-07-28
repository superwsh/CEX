const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("WBTC 和 USDT 代币测试", function () {
  let wbtc, usdt;
  let owner, addr1, addr2;

  beforeEach(async function () {
    [owner, addr1, addr2] = await ethers.getSigners();

    // 部署 WBTC 合约
    const WBTC = await ethers.getContractFactory("WBTC");
    wbtc = await WBTC.deploy();
    await wbtc.waitForDeployment();

    // 部署 USDT 合约
    const USDT = await ethers.getContractFactory("USDT");
    usdt = await USDT.deploy();
    await usdt.waitForDeployment();
  });

  describe("WBTC 代币", function () {
    it("应该有正确的名称和符号", async function () {
      expect(await wbtc.name()).to.equal("Wrapped Bitcoin");
      expect(await wbtc.symbol()).to.equal("WBTC");
      expect(await wbtc.decimals()).to.equal(8);
    });

    it("应该有正确的初始供应量", async function () {
      const totalSupply = await wbtc.totalSupply();
      expect(ethers.formatUnits(totalSupply, 8)).to.equal("1000000.0");
    });

    it("应该允许所有者铸币", async function () {
      const mintAmount = ethers.parseUnits("100", 8);
      await wbtc.mint(addr1.address, mintAmount);
      expect(await wbtc.balanceOf(addr1.address)).to.equal(mintAmount);
    });

    it("应该允许销毁代币", async function () {
      const burnAmount = ethers.parseUnits("1000", 8);
      await wbtc.burn(burnAmount);
      const expectedBalance = ethers.parseUnits("999000", 8);
      expect(await wbtc.balanceOf(owner.address)).to.equal(expectedBalance);
    });

    it("应该允许转账", async function () {
      const transferAmount = ethers.parseUnits("100", 8);
      await wbtc.transfer(addr1.address, transferAmount);
      expect(await wbtc.balanceOf(addr1.address)).to.equal(transferAmount);
    });
  });

  describe("USDT 代币", function () {
    it("应该有正确的名称和符号", async function () {
      expect(await usdt.name()).to.equal("Tether USD");
      expect(await usdt.symbol()).to.equal("USDT");
      expect(await usdt.decimals()).to.equal(6);
    });

    it("应该有正确的初始供应量", async function () {
      const totalSupply = await usdt.totalSupply();
      expect(ethers.formatUnits(totalSupply, 6)).to.equal("1000000000.0");
    });

    it("应该允许所有者铸币", async function () {
      const mintAmount = ethers.parseUnits("1000", 6);
      await usdt.mint(addr1.address, mintAmount);
      expect(await usdt.balanceOf(addr1.address)).to.equal(mintAmount);
    });

    it("应该允许销毁代币", async function () {
      const burnAmount = ethers.parseUnits("1000000", 6);
      await usdt.burn(burnAmount);
      const expectedBalance = ethers.parseUnits("999000000", 6);
      expect(await usdt.balanceOf(owner.address)).to.equal(expectedBalance);
    });

    it("应该允许转账", async function () {
      const transferAmount = ethers.parseUnits("1000", 6);
      await usdt.transfer(addr1.address, transferAmount);
      expect(await usdt.balanceOf(addr1.address)).to.equal(transferAmount);
    });

    it("应该允许管理黑名单地址", async function () {
      await usdt.addToBlacklist(addr1.address);
      expect(await usdt.isBlacklisted(addr1.address)).to.be.true;
      
      await usdt.removeFromBlacklist(addr1.address);
      expect(await usdt.isBlacklisted(addr1.address)).to.be.false;
    });

    it("应该阻止黑名单地址的转账", async function () {
      await usdt.addToBlacklist(owner.address);
      const transferAmount = ethers.parseUnits("1000", 6);
      
      await expect(
        usdt.transfer(addr1.address, transferAmount)
      ).to.be.revertedWith("USDT: transfer from blacklisted address");
    });
  });

  describe("暂停功能", function () {
    it("应该允许所有者暂停和恢复 WBTC", async function () {
      await wbtc.pause();
      const transferAmount = ethers.parseUnits("100", 8);
      
      await expect(
        wbtc.transfer(addr1.address, transferAmount)
      ).to.be.revertedWithCustomError(wbtc, "EnforcedPause");
      
      await wbtc.unpause();
      await wbtc.transfer(addr1.address, transferAmount);
      expect(await wbtc.balanceOf(addr1.address)).to.equal(transferAmount);
    });

    it("应该允许所有者暂停和恢复 USDT", async function () {
      await usdt.pause();
      const transferAmount = ethers.parseUnits("1000", 6);
      
      await expect(
        usdt.transfer(addr1.address, transferAmount)
      ).to.be.revertedWithCustomError(usdt, "EnforcedPause");
      
      await usdt.unpause();
      await usdt.transfer(addr1.address, transferAmount);
      expect(await usdt.balanceOf(addr1.address)).to.equal(transferAmount);
    });
  });
});
